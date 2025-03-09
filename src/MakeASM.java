import java.io.ByteArrayOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ir.*;
import ir.IRFunction.*;
import ir.datatype.IRArrayType;
import ir.operand.IRVariableOperand;


public class MakeASM {
    public static void main(String[] args) throws Exception {
        // System.out.println(System.getProperty("user.dir"));
        IRReader irReader = new IRReader();
        IRProgram program = irReader.parseIRFile(args[0]);
        IRProgram optimizedProgram = new IRProgram();

        PrintStream fileOut = new PrintStream(System.out);
        if (args[1].equals("--naive")) {
            NaiveAllocator allocator = new NaiveAllocator(fileOut);
            allocator.NaivePrintProgram(program);
        }
        if (args[1].equals("--greedy")) {
            for (IRFunction func : program.functions) {
                // System.out.println("Map for function: " + func.name);
                //build the def/reach map for the whole function
                HashMap<Integer, IRInstruction> varDefReachMap = Reaching.getVarDefReachMap(func);
                // System.out.println("\n Variable Definitions (varDefReachMap):");
                for (Integer defLine : varDefReachMap.keySet()) {
                    IRInstruction instr = varDefReachMap.get(defLine);
                    String varName = instr.getDefOperand().toString();
                    String defID = instr.getDefID();

                    //System.out.println("DefID: " + defID + " | Variable: " + varName + " | Defined at Line: " + defLine);
                }
                // System.out.println();
                BasicBlock head = new BasicBlock(func, varDefReachMap);
                Reaching.computeUseSet(head.getBlockList(), varDefReachMap);
                buildLiveSets(head);
                for (BasicBlock b : head.getBlockList()) {
                    //System.out.println("BB starting at line " + b.startLine + " and ending at line "+ b.endLine);
                    //System.out.println("LiveIn: " + b.getLiveIn());
                    //System.out.println("LiveOut: " + b.getLiveOut());
                }
                String greedyASM = makeGreedyASM(head, func);
                System.out.println(greedyASM);
            }
        }
        fileOut.close();
    }


    public static String makeGreedyASM(BasicBlock head, IRFunction func) {
        // add in calling convention, save registers, make sure we have enough room
        // on the stack for the number of variables (including a space for each index in arrays)
        // we will also need the parameters
        
        // we will need to get ASM for each block and decide the ordering based on start and end lines
        // It will probably be helpful for both greedy and naive to pass a map of the stack offset to
        // variable names.
        
        // and at the end add the calling convention for returning, restoring any saved registers
        try {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(outputStream);
        
        Set<String> paramNames = new HashSet<>();
        int local_size = 0;
        int arg_size = 0;
        HashMap<String, Integer> stackMap = new HashMap<String, Integer>();
        int fp_offset = 4;

        for (IRVariableOperand param : func.parameters) {
            if (paramNames.contains(param.getName()))
                continue;
            paramNames.add(param.getName());
            stackMap.put(param.getName(), fp_offset);
            //System.out.println(";; param: " + param.getName() + " has offset: " + fp_offset);            
            fp_offset += 4;
            arg_size += 4;
        }
        
        for (IRVariableOperand variable : func.variables) {
            if (paramNames.contains(variable.getName()))
                continue;
        
            local_size += 4;
            stackMap.put(variable.getName(), fp_offset);
            //System.out.println(";; var: " + variable.getName() + " has offset: " + fp_offset);
            fp_offset += 4;
        }
        
        local_size += arg_size;
        func.local_size = local_size;

        ps.print(func.name);
        ps.println(':');
        
        ps.println("    addi $sp, $sp, -4");
        ps.println("    sw   $fp, 0($sp)");
        ps.println("    move $fp, $sp");

        int argOffset = (func.parameters.size() - 4) * 4;
        for (int i = 0; i < func.parameters.size(); i++) {
            int offset = stackMap.get(func.parameters.get(i).getName());
            //ps.println(func.parameters.get(i));
            if (i < 4) {
                ps.println("    sw $a" + i + ", -" + offset + "($fp)");
            } else {
                ps.println("    lw $t0, " + argOffset + "($fp)");  
                argOffset -= 4;

                ps.println("    sw $t0, -" + offset + "($fp)");
            }
        }
        if (local_size > 0) {
            ps.println("    addi $sp, $sp, -" + local_size);
        }
       //MAYBE: align stack pointer to 32 bits 
        ps.println("    addi $sp, $sp, -4");

        ps.println("    sw   $ra, 0($sp)");
        
        // sbrk for array().
        // store address at local stack offset
        for (IRVariableOperand var : func.variables) {
            //System.out.println(var.getName());
            if (var.type instanceof IRArrayType && !func.parameters.contains(var)) {
                IRArrayType arr = (IRArrayType) var.type;
                ps.println("    li $v0, 9");
                ps.println("    li $a0, " + ((arr.getSize() * 4) + 4));
                ps.println("    syscall");
                ps.println("    sw $v0, -" + stackMap.get(var.getName()) + "($fp)");
            }
        }

        // get ASM by block
        HashMap<Integer, String> BlockASMStringOrder = new HashMap<Integer, String>();
        for (BasicBlock block : head.getBlockList()) {
            String blockasm = block.makeGreedyASM(stackMap);
            BlockASMStringOrder.put(block.startLine, blockasm);
        }

        // ensure we print the assembly in the correct order
        ArrayList<Integer> startLines = new ArrayList<>(BlockASMStringOrder.keySet());
        Collections.sort(startLines);
        for (int i = 0; i < startLines.size(); i++) {
            ps.print(BlockASMStringOrder.get(startLines.get(i)));
        }

        ps.println("    lw   $ra, 0($sp)");
        ps.println("    addi $sp, $sp, 4");
        if (local_size > 0) {
            ps.println("    addi $sp, $sp, " + local_size);
        }
        ps.println("    lw   $fp, 0($sp)");
        ps.println("    addi $sp, $sp, 4");

        if (func.name.equals("main")) {
            ps.println("    li $v0, 10");
            ps.println("    syscall");
        } else {
            ps.println("    jr $ra");
        }


        return outputStream.toString("UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    // takes the head of a function's CFG and computes the LiveIn/Out sets for each BB
    public static void buildLiveSets(BasicBlock head) {
        
        @SuppressWarnings("unchecked") // we know what the return type is
        ArrayList<BasicBlock> worklist = (ArrayList<BasicBlock>) head.getBlockList().clone();
        while (worklist.size() > 0) {
            BasicBlock block = worklist.remove(0);


            // we want to make sure that this list is not updated when computing the new one on the block
            Set<String> oldLiveIn = BasicBlock.cloneSet(block.getLiveIn());
            //System.out.println("iteration liveout @ " + block.startLine + ": "+ block.computeLiveOut());
            block.computeLiveOut();
            Set<String> newLiveIn = block.computeLiveIn();
            //System.out.println("iteration livein block @ " + block.startLine + ": " + newLiveIn);
            // if the LiveIn changed then add predecessors to the worklist.
            if (!newLiveIn.equals(oldLiveIn)) {
                worklist.addAll(block.pre);
            }
        }
    }
}


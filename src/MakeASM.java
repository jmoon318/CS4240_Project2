import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import ir.*;
import ir.IRFunction.*;
import ir.operand.IRVariableOperand;


public class MakeASM {
    public static void main(String[] args) throws Exception {
        // System.out.println(System.getProperty("user.dir"));
        IRReader irReader = new IRReader();
        IRProgram program = irReader.parseIRFile(args[0]);
        IRProgram optimizedProgram = new IRProgram();

        PrintStream fileOut = new PrintStream(System.out);
        NaiveAllocator allocator = new NaiveAllocator(fileOut);
        allocator.NaivePrintProgram(program);
        fileOut.close();


        for (IRFunction func : program.functions) {
            // System.out.println("Map for function: " + func.name);
            //build the def/reach map for the whole function
            HashMap<Integer, IRInstruction> varDefReachMap = Reaching.getVarDefReachMap(func);
            // System.out.println("\n Variable Definitions (varDefReachMap):");
            for (Integer defLine : varDefReachMap.keySet()) {
                IRInstruction instr = varDefReachMap.get(defLine);
                String varName = instr.getDefOperand().toString();
                String defID = instr.getDefID();

                // System.out.println("DefID: " + defID + " | Variable: " + varName + " | Defined at Line: " + defLine);
            }
            // System.out.println();
            BasicBlock head = new BasicBlock(func, varDefReachMap);
            Reaching.computeUseSet(head.getBlockList(), varDefReachMap);
            buildLiveSets(head);
            System.out.println("block list: " + head.getBlockList());
            for (BasicBlock b : head.getBlockList()) {
                System.out.println("BB starting at line " + b.startLine + " and ending at line "+ b.endLine);
                System.out.println("LiveIn: " + b.getLiveIn());
                System.out.println("LiveOut: " + b.getLiveOut());
            }
            String greedyASM = makeGreedyASM(head, func);

        }
    }

    public static String makeNaiveASM(BasicBlock head, IRFunction func) {
        // add in calling convention, save registers, make sure we have enough room
        // on the stack for the number of variables (including a space for each index in arrays)
        // we will also need the parameters
        
        // we will need to get ASM for each block and decide the ordering based on start and end lines
        // It will probably be helpful for both greedy and naive to pass a map of the stack offset to
        // variable names.
        
        // and at the end add the calling convention for returning, restoring any saved registers
        return null;
    }

    public static String makeGreedyASM(BasicBlock head, IRFunction func) {
        // add in calling convention, save registers, make sure we have enough room
        // on the stack for the number of variables (including a space for each index in arrays)
        // we will also need the parameters
        
        // we will need to get ASM for each block and decide the ordering based on start and end lines
        // It will probably be helpful for both greedy and naive to pass a map of the stack offset to
        // variable names.
        
        // and at the end add the calling convention for returning, restoring any saved registers
        return null;
    }

    // takes the head of a function's CFG and computes the LiveIn/Out sets for each BB
    public static void buildLiveSets(BasicBlock head) {
        
        @SuppressWarnings("unchecked") // we know what the return type is
        ArrayList<BasicBlock> worklist = (ArrayList<BasicBlock>) head.getBlockList().clone();
        while (worklist.size() > 0) {
            BasicBlock block = worklist.remove(0);


            // we want to make sure that this list is not updated when computing the new one on the block
            Set<String> oldLiveIn = BasicBlock.cloneSet(block.getLiveIn());
            System.out.println("iteration liveout @ " + block.startLine + ": "+ block.computeLiveOut());
            Set<String> newLiveIn = block.computeLiveIn();
            System.out.println("iteration livein block @ " + block.startLine + ": " + newLiveIn);
            // if the LiveIn changed then add predecessors to the worklist.
            if (!newLiveIn.equals(oldLiveIn)) {
                worklist.addAll(block.pre);
            }
        }
    }
}


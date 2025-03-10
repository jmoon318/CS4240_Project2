
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import ir.IRFunction;
import ir.IRInstruction;
import ir.IRPrinter;
import ir.IRInstruction.OpCode;
import ir.operand.IRConstantOperand;
import ir.operand.IRLabelOperand;
import ir.operand.IROperand;
import ir.operand.IRVariableOperand;


public class BasicBlock {
    private static int blockCounter = 0;
    private int blockID;
    public int startLine;
    public int endLine;
    public ArrayList<BasicBlock> pre;
    public ArrayList<IRInstruction> instructions;
    public ArrayList<BasicBlock> next;
    public static ArrayList<BasicBlock> blockList;
    // maps the kill set as a list of variables and the line(s) they are defined on
    public HashMap<String, ArrayList<Integer>> killSet;
    // genSet done the same way
    public HashMap<String, ArrayList<Integer>> genSet;
    // outSet done the same way
    public HashMap<String, ArrayList<Integer>> outSet;
    public HashMap<String, ArrayList<Integer>> inSet;
    public IRFunction func;
    private Set<String> liveIn;
    private Set<String> liveOut;

    public BasicBlock(int blockID, int startLine, int endLine) {
        this.blockID = blockID;
        this.startLine = startLine;
        this.endLine = endLine;
        this.instructions = new ArrayList<>();
        this.pre = new ArrayList<>();
        this.next = new ArrayList<>();
        this.genSet = new HashMap<>();
        this.killSet = new HashMap<>();
        this.inSet = new HashMap<>();
        this.outSet = new HashMap<>();
        this.liveIn = new HashSet<String>();
        this.liveOut = new HashSet<String>();
    }

    /**
     * constructor makes a CFG with the returning BB as the head of the function's CFG (function header)
     * CFG is stored as a linked list of BBs with .pre holding a list of predecessors and .next holding a list of
     * the following nodes
     * @param func IR function object from IRReader
     */
    public BasicBlock(IRFunction func, HashMap<Integer, IRInstruction> defmap) {
        this.func = func;
        ArrayList<Integer> startLineNos = new ArrayList<>();
        ArrayList<Integer> endLineNos = new ArrayList<>();
        this.instructions = new ArrayList<>();
        boolean flag = false;

        // System.out.println("\n"+func.returnType+"\n");

        for (int i = 0; i < func.instructions.size(); i++) {
            IRInstruction instr = func.instructions.get(i);
            if (isLeader(instr, i, func)) {
                startLineNos.add(instr.irLineNumber);

                if (i == func.instructions.size() - 1) {
                    endLineNos.add(instr.irLineNumber);
                }

            }
        }

        for (int i = 0; i < startLineNos.size(); i++) {
            int nextLine = startLineNos.get(i) + 1;
            int nextIdx = nextLine - func.instructions.get(0).irLineNumber;
            while (nextIdx < func.instructions.size()) {
                IRInstruction nextInstr = func.instructions.get(nextIdx);
                if (isLeader(nextInstr, nextIdx, func)) {
                    endLineNos.add(nextLine - 1);
                    break;
                } else if (nextIdx == func.instructions.size() - 1) {
                    endLineNos.add(nextLine);
                    break;
                }
                nextLine++;
                nextIdx++;
            }
        }

        if (startLineNos.size() != endLineNos.size()) {
            endLineNos.add(0, startLineNos.get(startLineNos.size() - 1));
        }

        int instrOffset = func.instructions.get(0).irLineNumber;
        blockList = new ArrayList<>();
        startLineNos.sort(null);
        endLineNos.sort(null);
        for (int i = 0; i < startLineNos.size(); i++) {
            int startLine = startLineNos.get(i);
            int endLine = endLineNos.get(i);
            BasicBlock newBlock = new BasicBlock(blockID, startLine, endLine);
            newBlock.func = this.func;
            // System.out.println("start nos: " + startLineNos + "\nand end: " + endLineNos);
            // System.out.println("new block with start: " + startLine + "\nand end: " + endLine);

            newBlock.setBlockID(blockID);
            // System.out.println("instr offset: " + instrOffset + " instructions size: " + func.instructions.size());
            for (int j = startLineNos.get(i); j <= endLineNos.get(i); j++) {
                IRInstruction instr = func.instructions.get(j - instrOffset);
                // System.out.println("instruction: " + instr.opCode + " from line " + instr.irLineNumber + " added");
                instr.setAssignedBlock(blockID);
                newBlock.instructions.add(instr);
            }

            blockList.add(newBlock);
        }

        for (BasicBlock block : blockList) {
                IRInstruction firstInstr = block.instructions.get(0);
                IRInstruction lastInstr = block.instructions.get(block.instructions.size() - 1);

                if (lastInstr.opCode == OpCode.GOTO) {
                    String label = lastInstr.operands[0].toString();
                    BasicBlock nextLabel = labelStart(label, blockList);
                    block.next.add(nextLabel);
                    nextLabel.pre.add(block);

                } else if (isBranch(lastInstr)) {
                    String label = lastInstr.operands[0].toString();
                    BasicBlock nextLabel = labelStart(label, blockList);
                    block.next.add(nextLabel);
                    nextLabel.pre.add(block);

                    BasicBlock nextLine = lineStart(lastInstr.irLineNumber, blockList);
                    if (nextLine != null) {
                        block.next.add(nextLine);
                        nextLine.pre.add(block);
                    }
                } else {
                    BasicBlock nextLine = lineStart(lastInstr.irLineNumber, blockList);
                    if (nextLine != null) {
                        block.next.add(nextLine);
                        nextLine.pre.add(block);
                    }
                }

                block.genSet = block.getGenSet();
                block.killSet = block.getKillSet(defmap);
                block.outSet = new HashMap<>(block.genSet);
                block.inSet = new HashMap<>();
                block.blockID = blockCounter++;

                printBlockSets(block);
        }
        computeFixedPoint(func);



        BasicBlock head = lineStart(instrOffset - 1, blockList);
        this.next = head.next;
        this.instructions = head.instructions;
        this.pre = head.pre;
        this.genSet = head.genSet;
        this.killSet = head.killSet;
        this.outSet = head.outSet;


    }

    public int getBlockID() {
        return this.blockID;
    }

    public void setBlockID(int blockID) {
        this.blockID = blockID;
    }

    public ArrayList<BasicBlock> getBlockList() {
        return this.blockList;
    }


    private BasicBlock labelStart(String label, ArrayList<BasicBlock> blockList) {
        for (BasicBlock block : blockList) {
            IRInstruction firstInstr = block.instructions.get(0);
            if (firstInstr.opCode == OpCode.LABEL) {
                if (firstInstr.operands[0].toString().contains(label)) {
                    //// System.out.println("found block starting at line: " + firstInstr.irLineNumber);
                    return block;
                }
            }
        }
        return null;
    }

    private BasicBlock lineStart(Integer line, ArrayList<BasicBlock> blockList) {
         for (BasicBlock block : blockList) {
            if (block.instructions.size() > 0) {
                IRInstruction firstInstr = block.instructions.get(0);
                if (firstInstr.irLineNumber == line + 1) {
                    //// System.out.println("found block starting at line: " + firstInstr.irLineNumber);
                    return block;
                }
            }
        }
        return null;       
    }

    private Boolean isLeader(IRInstruction instr, Integer instrIdx, IRFunction func) {
        if (instrIdx == 0) {
            return true;
        }
        
        IRInstruction prevInstr = func.instructions.get(instrIdx - 1);
        return instr.opCode == OpCode.LABEL || 
        prevInstr.opCode == OpCode.BREQ ||
        prevInstr.opCode == OpCode.BRNEQ ||
        prevInstr.opCode == OpCode.BRLT || 
        prevInstr.opCode == OpCode.BRGT || 
        prevInstr.opCode == OpCode.BRGEQ;
    }

    private Boolean isBranch(IRInstruction instr) {
        return instr.opCode == OpCode.BREQ ||
        instr.opCode == OpCode.BRNEQ ||
        instr.opCode == OpCode.BRLT || 
        instr.opCode == OpCode.BRGT || 
        instr.opCode == OpCode.BRGEQ;
    }


    private HashMap<String, ArrayList<Integer>> getKillSet(HashMap<Integer, IRInstruction> defmap) {
        HashMap<String, ArrayList<Integer>> killSet = new HashMap<>();

        for (String var : this.genSet.keySet()) {
            ArrayList<Integer> defsToKill = new ArrayList<>();

            for (Integer defLine : defmap.keySet()) {
                IRInstruction defInstr = defmap.get(defLine);
                if (defInstr.getDefOperand().toString().equals(var)) {
                    if (defInstr.getAssignedBlock() != this.getBlockID()) {
                        defsToKill.add(defLine);
                    } 
                }
            }
            killSet.put(var, defsToKill);
        }

        return killSet;
    }

    private boolean isFinalAssignment(String var, Integer lineNo) {
        int maxLine = this.instructions.get(this.instructions.size() - 1).irLineNumber;
        int instrCount = this.instructions.size();
        for (int instrIdx = instrCount - 1; instrIdx >= 0; instrIdx--) {
            IRInstruction i = this.instructions.get(instrIdx);
            if (isDefinitionOp(i.opCode) && i.getDefOperand().toString().equals(var)) {
                    maxLine = i.irLineNumber;
                    return maxLine == lineNo;
            }
        }
        return false;
    }
   private static boolean isDefinitionOp(OpCode opCode) {
        return opCode == OpCode.ADD || opCode == OpCode.SUB ||
                opCode == OpCode.MULT || opCode == OpCode.DIV ||
                opCode == OpCode.AND || opCode == OpCode.OR ||
                opCode == OpCode.ASSIGN || opCode == OpCode.CALLR ||
                opCode == OpCode.ARRAY_STORE || opCode == OpCode.ARRAY_LOAD;
    }
    private HashMap<String, ArrayList<Integer>> getGenSet() {
        HashMap<String, ArrayList<Integer>> genSet = new HashMap<>();

        for (IRInstruction instr : this.instructions) {
            // Check if the instruction is part of the GEN set
            if (instr.gen) { // Assume `gen` is a boolean field in IRInstruction

                // Ensure operands array is not null and has at least one element
                if (instr.operands != null && instr.operands.length > 0) {
                    String variableName = instr.getDefOperand().toString(); // Get the defined variable
                    int lineNumber = instr.irLineNumber; // Get the line number of the instruction

                    // Add the variable to the GEN set
                    if (this.isFinalAssignment(variableName, lineNumber)) {
                        genSet.computeIfAbsent(variableName, k -> new ArrayList<>()).add(lineNumber);
                    } else {
                        // System.out.println("NOT FINAL" + lineNumber + " variable: " + variableName); 
                    }
                }
            }
        }

        return genSet;
    }

    private BasicBlock() {
        pre = new ArrayList<BasicBlock>();
        instructions = new ArrayList<IRInstruction>();
        next = new ArrayList<BasicBlock>();
    }


    public HashMap<String, ArrayList<Integer>> getOutSet() {
        return this.outSet;
    }


    public void computeFixedPoint(IRFunction func) {
        int maxIterations = 4;

        BasicBlock headBlock = blockList.get(0);

        for (IRVariableOperand param : func.parameters) {
            String paramName = param.getName();
            Integer paramDefLine = func.instructions.get(0).irLineNumber - 3;
            headBlock.inSet.computeIfAbsent(paramName, k -> new ArrayList<>())
                    .add(paramDefLine);
        }

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            // System.out.println("Iteration " + iteration);

            for (BasicBlock block : blockList) {
                HashMap<String, HashSet<Integer>> newInSet = new HashMap<>();

                for (BasicBlock pred : block.pre) {
                    for (String var : pred.outSet.keySet()) {
                        newInSet.computeIfAbsent(var, k -> new HashSet<>())
                                .addAll(pred.outSet.get(var));
                    }
                }

                for (String var : newInSet.keySet()) {
                    block.inSet.put(var, new ArrayList<>(newInSet.get(var)));
                }

                HashMap<String, HashSet<Integer>> newOutSet = new HashMap<>();

                for (String var : block.genSet.keySet()) {
                    newOutSet.computeIfAbsent(var, k -> new HashSet<>())
                            .addAll(block.genSet.get(var));
                }

                for (String var : block.inSet.keySet()) {
                    if (!block.killSet.containsKey(var)) {
                        newOutSet.computeIfAbsent(var, k -> new HashSet<>())
                                .addAll(block.inSet.get(var));
                    }
                }

                block.outSet.clear();
                for (String var : newOutSet.keySet()) {
                    block.outSet.put(var, new ArrayList<>(newOutSet.get(var)));
                }

                printBlockSets(block);
            }
        }

        // System.out.println("Fixed Point Computation completed after " + maxIterations + " iterations.");
    }

    public InterferenceGraph getIGraph() {
        return new InterferenceGraph(this);
    }

    public Set<String> getUEVars() {
        HashSet<String> out = new HashSet<String>();
        for (IRInstruction instr : this.instructions) {
            for (String usedDef : instr.getUseSet()) {
                //System.out.println("used def: " + usedDef);
                int defLine = Integer.valueOf(usedDef.substring(3, usedDef.indexOf("(")));
                if (defLine < this.startLine) {
                    IROperand uevar = this.func.getInstruction(defLine).getDefOperand();
                    out.add(uevar.toString());
                }
            }
            for (IROperand operand : instr.getUsedVars()) {
                if (operand instanceof IRVariableOperand && 
                    !defBefore(operand.toString(), instr.irLineNumber)) {

                    out.add(operand.toString());
                }
            }
        }
        return out;
    }
    
    // computes the LiveIn set returns the updated set
    public Set<String> computeLiveOut() {
        HashSet<String> out = new HashSet<String>();
        for (BasicBlock successor : this.next) {
            for (String var : successor.getLiveIn()) {
                out.add(var);
            }
        }
        this.liveOut = out;
        return this.liveOut;
    }

    // gets the LiveOut set without modification
    public Set<String> getLiveOut(){
        return this.liveOut;
    }

    // computes the LiveIn set returns the updated set
    public Set<String> computeLiveIn() {
        HashSet<String> out = new HashSet<String>();
        out.addAll(this.getUEVars());
        // we want to get a clone as to not change the current state of liveOut
        HashSet<String> liveOut = cloneSet(this.liveOut);
        for (IRInstruction killInstr: this.getKillInstructions()) {
            // remove operands that are in this block's kill set
            liveOut.remove(killInstr.getDefOperand().toString());
        }
        out.addAll(liveOut);
        this.liveIn = out;
        return this.liveIn;
    }

    // gets the LiveIn set without modification
    public Set<String> getLiveIn(){
        return this.liveIn;
    }

    // returns a list of instructions in this block that contribute to the kill set
    public ArrayList<IRInstruction> getKillInstructions() {
        ArrayList<IRInstruction> out = new ArrayList<IRInstruction>();
        for (String killVar : this.killSet.keySet()) {
            for (IRInstruction instr : this.instructions) {
                if (instr.getDefOperand().toString().equals(killVar)) {
                    out.add(instr);
                }
            }
        }
        return out;
    }

    public static HashSet<String> cloneSet(Set<String> set) {
        HashSet<String> out = new HashSet<String>();
        for (String e : set) {
            out.add(e);
        }
        return out;
    }

    public boolean defBefore(String var, int lineNo) {
        lineNo -= 1;
        while (lineNo >= this.startLine) {
            if (var.equals(this.func.getInstruction(lineNo--).getDefOperand().toString())) {
                return true;
            }
        }
        //for (IRVariableOperand varOperand : this.func.parameters) {
        //    if (varOperand.toString().equals(var)) {
        //        return true;
        //    }
        //}
        return false;
    }

    public void printBlockSets(BasicBlock block) {
        // System.out.println("BasicBlock #" + block.getBlockID() + " [Lines: " + block.startLine + " - " + block.endLine + "]");
        // System.out.println("Kill set: " + block.killSet);
        //System.out.print("  IN: ");
        printSetWithDefID(block.inSet);

        //System.out.print("  OUT: ");
        printSetWithDefID(block.outSet);

        // System.out.println();
    }

    private void printSetWithDefID(HashMap<String, ArrayList<Integer>> set) {
        if (set == null || set.isEmpty()) {
            // System.out.println("{}");
            return;
        }

        //System.out.print("{ ");
        boolean first = true;
        for (String var : set.keySet()) {
            if (!first) //System.out.print(", ");
            //System.out.print(var + ": " + set.get(var));
            first = false;
        }
        //System.out.print(" }\n");
    }

    public HashMap<String, Integer> getVarUseSet() {
        HashMap<String, Integer> out = new HashMap<>();
        for (IRInstruction instr : this.func.instructions) {
            for (IROperand operand : instr.operands) {
                if (operand instanceof IRVariableOperand) {
                    String name = operand.getValue();
                    if (out.get(name) != null) {
                        out.put(name, out.get(name) + 1);
                    } else {
                        out.put(name, 1);
                    }
                }
            }
        }
        return out;
    }

    public ArrayList<String> getUseOnly() {
        ArrayList<String> out = new ArrayList<>();
        for (IRInstruction instr : this.instructions) {
            if (isDefinitionOp(instr.opCode)) {
                String name = instr.getDefOperand().getValue();
                for (IROperand operand : instr.operands) {
                    if (!(operand.getValue().equals(name) || 
                        operand instanceof IRConstantOperand || 
                        operand instanceof IRLabelOperand)) {
                        out.add(name);
                    }
                }
            }
        }
        return out;
 
    }

    public ArrayList<String> getVarDefs() {
        ArrayList<String> out = new ArrayList<>();
        for (IRInstruction instr : this.instructions) {
            if (isDefinitionOp(instr.opCode)) {
                String name = instr.getDefOperand().getValue();
                if (!out.contains(name)) {
                    out.add(name);
                }
            }
        }

        return out;
    }

    public HashMap<String, Integer> getRegMap() {
        HashMap<String, Integer> worklist = getVarUseSet();
        //for (String liveVar : this.getLiveIn()) {
        //    if (worklist.get(liveVar) == null) {
        //        worklist.put(liveVar, 1);
        //    }
        //}
        HashMap<String, Integer> out = new HashMap<>();
        int max_use = 0;
        String max_var = "";
        int reg = 0;
        while(worklist.size() > 0) {
            for (String var : worklist.keySet()) {
                if (worklist.get(var) > max_use) {
                    max_use = worklist.get(var);
                    max_var = var;
                }
            }
            out.put(max_var, reg++);
            worklist.remove(max_var);
            max_use = 0;
            max_var = "";
        }

        return out;
    }

    public String makeGreedyASM(HashMap<String, Integer> stackMap) {
        // start by adding the code to load in the LiveIn variables from stack to registers.
        // I think that if a variable doesn't appear in any of the livein/out sets then its an internal
        // value to that BB and does not need space on the stack.

        // the register assignments can be obtained from the interference graph with `iGraph.getRegMap()`
        // this gives a HashMap<String, Integer> where the String is the variable name and Integer is the
        // coorespoding register t0-t9. If the integer is 10 then this is spilled.

        // run instruction selection, making sure that the appropriate variables are replaced
        // with the cooresponding registers loaded with LiveIn.
        // Variables that overflow/spill will use the same load/store logic as the naive approach.

        // store the LiveOut variables to their locations on the stack
        
        ByteArrayOutputStream outputStream;
        PrintStream ps;
        
        try {
            outputStream = new ByteArrayOutputStream();
            ps = new PrintStream(outputStream, true, StandardCharsets.UTF_8.name());
        
            GreedyAllocator gAlloc = new GreedyAllocator(ps, this, stackMap);
            for (String virtReg : this.getVarUseSet().keySet()) {
            //System.out.println(";; virt: " + virtReg + " phys: " + this.getRegMap().get(virtReg));
            }
        for (String virtReg : this.liveIn) {
            //System.out.println(";; virt: " + virtReg + " phys: " + this.getRegMap().get(virtReg));

        }
        int start = 0;
        boolean save = true;
        if (this.instructions.get(0).opCode == OpCode.LABEL) {
            gAlloc.printInstruction(this.instructions.get(0), this.func);
            if (this.instructions.size() > 1) { 
                IRInstruction next = this.instructions.get(1);
                if (this.instructions.size() == 1) {
                    return outputStream.toString("UTF-8");
                } else if (next.opCode == OpCode.GOTO ||
                        next.opCode == OpCode.BREQ || 
                        next.opCode == OpCode.BRNEQ ||
                        next.opCode == OpCode.BRLT || 
                        next.opCode == OpCode.BRGT ||
                        next.opCode == OpCode.BRGEQ) {
                            save = false;
                }
            }
            start = 1;
        }

        //for (String virtReg : this.getVarUseSet().keySet()) {
        //    int regno = this.getRegMap().get(virtReg);
        //    if (regno >= 0 && regno <= 7) {
        //        String pReg = "$t" + regno;
        //        int stackOff = stackMap.get(virtReg);
        //        ps.println("    lw " + pReg + ", -" + stackOff + "($fp)");
        //    } // if our virtual register was spilled it will be loaded from memory when needed.
        //}
        
        
        IRInstruction final_instr = null;
        for (int i = start; i < this.instructions.size(); i++) {
            IRInstruction instr = this.instructions.get(i);
            if  (instr.opCode == OpCode.GOTO ||
                instr.opCode == OpCode.BREQ || 
                instr.opCode == OpCode.BRNEQ ||
                instr.opCode == OpCode.BRLT || 
                instr.opCode == OpCode.BRGT ||
                instr.opCode == OpCode.BRGEQ) {
                    final_instr = instr;
            } else {
                gAlloc.printInstruction(instr, this.func);
            }
        }        
            //for (String virtReg : this.liveOut) {
            //    if (this.getVarDefs().contains(virtReg)) {
            //        int regno = this.getRegMap().get(virtReg);
            //        if (regno >= 0 && regno <= 7) {
            //            String pReg = "$t" + regno;
            //            int stackOff = stackMap.get(virtReg);
            //            ps.println("    sw " + pReg + ", -" + stackOff + "($fp)");
            //        } // if our virtual register was spilled then it should already have been written to memory.
            //    }
            //}
        if (final_instr != null) {
            gAlloc.printInstruction(final_instr, this.func);
        }
        return outputStream.toString("UTF-8");
        } catch (Exception e) {
            System.out.println(e.fillInStackTrace());
            System.out.println(e);
            return "";
        }
    }

}
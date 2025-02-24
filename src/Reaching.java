import java.io.FileWriter;
import java.io.PrintStream;
import java.util.*;

import ir.IRFunction;
import ir.IRInstruction;
import ir.IRPrinter;
import ir.IRInstruction.OpCode;
import ir.IRProgram;
import ir.IRReader;
import ir.operand.*;

public class Reaching {

    public static void main(String[] args) throws Exception {
        // System.out.println(System.getProperty("user.dir"));
        IRReader irReader = new IRReader();
        IRProgram program = irReader.parseIRFile(args[0]);
        IRProgram optimizedProgram = new IRProgram();
        for (IRFunction func : program.functions) {
            // System.out.println("Map for function: " + func.name);
            //build the def/reach map for the whole function
            HashMap<Integer, IRInstruction> varDefReachMap = getVarDefReachMap(func);
            // System.out.println("\n Variable Definitions (varDefReachMap):");
            for (Integer defLine : varDefReachMap.keySet()) {
                IRInstruction instr = varDefReachMap.get(defLine);
                String varName = instr.getDefOperand().toString();
                String defID = instr.getDefID();

                // System.out.println("DefID: " + defID + " | Variable: " + varName + " | Defined at Line: " + defLine);
            }
            // System.out.println();


            BasicBlock head = new BasicBlock(func, varDefReachMap);
            computeUseSet(head.getBlockList(), varDefReachMap);
            // printing the head of the CFG will result in the rest of the CFG being printed
            // see BasicBlock.java to string.
            for (BasicBlock block : head.getBlockList()) {
                for (IRInstruction instr : block.instructions) {
                    // System.out.println("Instruction at Line " + instr.irLineNumber +
                    //         " uses Defs: " + instr.getUseSet());
                }
                System.out.println(block.getIGraph().toString());
            }
            ArrayList<Integer> keepLines = markLines(func);
            IRFunction optimizedFunction = optimize(func, keepLines);
            optimizedProgram.functions.add(optimizedFunction);
        }
        PrintStream fileOut = new PrintStream(System.out);
        IRPrinter irOptimized = new IRPrinter(fileOut);
        irOptimized.printProgram(optimizedProgram);
        fileOut.close();

    }

    public static HashMap<Integer, IRInstruction> getVarDefReachMap(IRFunction func) {
        HashMap<String, HashMap<Integer, ArrayList<Integer>>> varToDefs = new HashMap<>();
        HashMap<Integer, IRInstruction> genInstructions = new HashMap<>();

        int defCount = 1;

        for (IRVariableOperand param : func.parameters) {
            HashMap<Integer, ArrayList<Integer>> defToUses = new HashMap<>();
            ArrayList<Integer> uses = new ArrayList<>();
            Integer funcLine = (func.instructions.get(0).irLineNumber - 3);
            defToUses.put(funcLine, uses);
            varToDefs.put(param.getName(), defToUses);
        }

        // System.out.println();

        if (!func.parameters.isEmpty()) {
            for (int i = 0; i < func.parameters.size(); i++) {
                IRVariableOperand param = func.parameters.get(i);
                String defOperand = param.toString();
                Integer defLine = func.instructions.get(0).irLineNumber - 3;

                String defID = "def" + defLine++;  // def1, def2, def3...

                IRInstruction fakeInstr = new IRInstruction(OpCode.ASSIGN,  new IROperand[]{param}, defLine);
                fakeInstr.setDefID(defID);
                fakeInstr.setGen(true);

                genInstructions.put(defLine, fakeInstr);

                HashMap<Integer, ArrayList<Integer>> defToUses = varToDefs.getOrDefault(defOperand, new HashMap<>());
                defToUses.put(defLine, new ArrayList<>());
                varToDefs.put(defOperand, defToUses);
            }
        }
//
        for (IRInstruction instr : func.instructions) {
            if (isDefinitionOp(instr.opCode)) {
                String defOperand = instr.getDefOperand().toString();
                Integer defLine = instr.irLineNumber;
                String defID = "def" + defLine; //defCount++;  // def1, def2, def3...


                instr.setDefID(defID);
                instr.setGen(true);

                genInstructions.put(defLine, instr);

                HashMap<Integer, ArrayList<Integer>> defToUses = varToDefs.getOrDefault(defOperand, new HashMap<>());
                defToUses.put(defLine, new ArrayList<>());
                varToDefs.put(defOperand, defToUses);
            }
            if(isCriticalOp(instr)) {
                instr.setCritical(true);
                instr.setMark(true);
                func.worklist.add(instr);
                // System.out.println("Added critical instr to worklist: " + instr.opCode + " at line " + instr.irLineNumber);
            }

            for (IROperand op : instr.operands) {
                if (!op.toString().equals(instr.getDefOperand().toString())) {
                    HashMap<Integer, ArrayList<Integer>> defToUses = varToDefs.get(op.toString());
                    if (defToUses != null) {
                        Integer recentDef = defToUses.keySet().stream().max(Integer::compare).orElse(0);
                        defToUses.get(recentDef).add(instr.irLineNumber);
                    }
                }
            }
        }

        printGenSet(genInstructions);

        return genInstructions;
    }


    public static void computeUseSet(
            ArrayList<BasicBlock> blockList,
            HashMap<Integer, IRInstruction> defMap
    ) {
        for (BasicBlock block : blockList) {
            for (int i = 0; i < block.instructions.size(); i++) {
                IRInstruction instr = block.instructions.get(i);
                String definedVar = instr.getDefOperand().toString();

                for (int opIdx = 0; opIdx < instr.operands.length; opIdx++) {
                    String operandName = instr.operands[opIdx].toString();

                    // generally the operand at index 0 is a variable we are writing to
                    // when the instruction is return index 0 is the variable used to return
                    // when arr_store is the instruction then index 0 is the value we are trying to store
                    if (opIdx == 0 && instr.opCode != OpCode.RETURN 
                                    && instr.opCode != OpCode.ARRAY_STORE) {
                        continue;
                    } else if(opIdx == 1 && instr.opCode == OpCode.ARRAY_STORE) {
                        continue;
                    }

                    if (block.inSet.containsKey(operandName)) {
                        // System.out.println("inset:" +block.inSet);
                        //// System.out.println("operandName:" +operandName);

                        ArrayList<Integer> reachingDefs = new ArrayList<>(block.inSet.get(operandName));
                        Integer closestDefLine = null;

                        for (int j = 0; j < i; j++) {
                            IRInstruction prevInstr = block.instructions.get(j);
                            if (prevInstr.getDefOperand().toString().equals(operandName)) {
                                closestDefLine = prevInstr.irLineNumber;
                            }
                        }

                        if (closestDefLine != null) {
                            IRInstruction defInstr = defMap.get(closestDefLine);
                            // System.out.println(defInstr);

                            if (defInstr != null && defInstr.getDefOperand().toString().equals(operandName)) {
                                instr.addUse(defInstr.getDefID()+ '('+ defInstr.getDefOperand()+')');
                            }
                        } else {
                            for (Integer defLine : reachingDefs) {
                                IRInstruction defInstr = defMap.get(defLine);
                                if (defInstr != null && defInstr.getDefOperand().toString().equals(operandName)) {
                                    instr.addUse(defInstr.getDefID()+ '('+ defInstr.getDefOperand()+')');
                                }
                            }
                        }
                    } else {
                        Integer closestDefLine = null;
                        for (int j = 0; j < i; j++) {
                            IRInstruction prevInstr = block.instructions.get(j);
                            if (prevInstr.getDefOperand().toString().equals(operandName)) {
                                closestDefLine = prevInstr.irLineNumber;
                            }
                        }

                        if (closestDefLine != null) {
                            IRInstruction defInstr = defMap.get(closestDefLine);
                            if (defInstr != null && defInstr.getDefOperand().toString().equals(operandName)) {
                                instr.addUse(defInstr.getDefID()+ '('+ defInstr.getDefOperand()+')');
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isDefinitionOp(OpCode opCode) {
        return opCode == OpCode.ADD || opCode == OpCode.SUB ||
                opCode == OpCode.MULT || opCode == OpCode.DIV ||
                opCode == OpCode.AND || opCode == OpCode.OR ||
                opCode == OpCode.ASSIGN || opCode == OpCode.CALLR ||
                opCode == OpCode.ARRAY_LOAD;
    }

    private static boolean isCriticalOp(IRInstruction instr) {
        OpCode opCode = instr.opCode;
        if (opCode == OpCode.ASSIGN && instr.operands.length == 3) {
            return true;
        }
        return opCode == OpCode.GOTO ||
                opCode == OpCode.BREQ || opCode == OpCode.BRNEQ ||
                opCode == OpCode.BRLT || opCode == OpCode.BRGT ||
                opCode == OpCode.BRGEQ || opCode == OpCode.RETURN ||
                opCode == OpCode.CALL || opCode == OpCode.CALLR ||
                opCode == OpCode.LABEL || opCode == OpCode.ARRAY_STORE;
    }

    private static void printGenSet(HashMap<Integer, IRInstruction> genInstructions) {
        // System.out.println("GenSet Instructions:");
        for (Integer line : genInstructions.keySet()) {
            IRInstruction instr = genInstructions.get(line);
            // System.out.println("Line " + line + " -> " + instr.getDefID() + " (" + instr.opCode + ")");
        }
    }

    public static void printUseSet(HashMap<String, ArrayList<Integer>> useMap) {
        // System.out.println("\n Use Set:");

        if (useMap.isEmpty()) {
            // System.out.println("No uses found.");
            return;
        }

        for (String variable : useMap.keySet()) {
            ArrayList<Integer> useLines = useMap.get(variable);
            // System.out.println("Variable '" + variable + "' is used at lines: " + useLines);
        }
    }

    // when this function is called the worklist will have the critical instructions 
    public static ArrayList<Integer> markLines(IRFunction func) {
        ArrayList<Integer> out = new ArrayList<Integer>();
        // while worklist is not empty get a line and check the used variables for the following:
        while (func.worklist.size() > 0) {
            // pop the last item in the worklist
            IRInstruction currLine = func.worklist.get(func.worklist.size() - 1);
            func.worklist.remove(func.worklist.size() - 1);
            // we want to mark each of the defs used by this worklist item
            for (String defID : currLine.getUseSet()) {
                IRInstruction defInstr = getDefInstruction(defID, func);
                // check the definition line is not already marked
                // if it is unmarked we need to mark it
                if (!defInstr.mark) {
                    defInstr.setMark(true);
                    // add to worklist
                    func.worklist.add(defInstr);
                }
            }
            // for 
            // now all neccecary instructions are marked.
            // loop through function and add the marked instruction lines to the output
            for (IRInstruction instr : func.instructions) {
                if (instr.mark) {
                    out.add(instr.irLineNumber);
                }
            }
        }
        return out;
    }

    public static IRInstruction getDefInstruction(String defID, IRFunction func) {
        int lineNoEnd = defID.indexOf("(");
        // always expecting the first 3 charachters to be "def"
        String lineString = defID.substring(3, lineNoEnd);
        // get the line no of the first instruction.
        int funcOffset = func.instructions.get(0).irLineNumber;
        Integer lineNo = Integer.valueOf(lineString);
        Integer instrIdx = lineNo - funcOffset;
        return func.instructions.get(instrIdx);
    }

    public static IRFunction optimize(IRFunction func, ArrayList<Integer> keepLines) {
        ArrayList<IRInstruction> optimizedIrInstructions = new ArrayList<IRInstruction>();
        for (IRInstruction line : func.instructions) {
            if (keepLines.contains(Integer.valueOf(line.irLineNumber))) {
                optimizedIrInstructions.add(line);
            }
        }
        func.instructions = optimizedIrInstructions;
        return func;
    }

}



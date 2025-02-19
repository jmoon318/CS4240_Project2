import ir.*;
import ir.datatype.IRArrayType;
import ir.datatype.IRIntType;
import ir.datatype.IRType;
import ir.operand.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class IRInterpreter {

    public static void main(String[] args) throws Exception {
        IRInterpreter irInterpreter = new IRInterpreter(args[0]);

        irInterpreter.run();

        Stats stats = irInterpreter.getStats();
        System.err.println("Number of non-label instructions executed: " + stats.getNonLabelInstructionCount());
    }

    private class StackFrame {
        public IRFunction caller;
        public IRInstruction callInst;
        public int returnInstIdx;
        public IRFunction function;
        public Map<String, Object> varMap;

        public Object getVal(IRVariableOperand variable) {
            return varMap.get(variable.getName());
        }

        public void setVal(IRVariableOperand variable, Object val) {
            varMap.put(variable.getName(), val);
        }
    }

    private class ProgramCounter {
        private ArrayList<IRInstruction> currentInstList;
        private int nextIdx;

        public void set(ArrayList<IRInstruction> instList, int nextIdx) {
            this.currentInstList = instList;
            this.nextIdx = nextIdx;
        }

        public void setNextIdx(int nextIdx) {
            this.nextIdx = nextIdx;
        }

        public int getNextIdx() {
            return nextIdx;
        }

        private IRInstruction next() {
            IRInstruction inst = currentInstList.get(nextIdx);
            nextIdx++;
            return inst;
        }

        private boolean hasNext() {
            return nextIdx < currentInstList.size();
        }
    }

    public class Stats {
        public int totalInstructionCount;
        public Map<IRInstruction.OpCode, Integer> instructionCounts;

        public Stats() {
            totalInstructionCount = 0;
            instructionCounts = new HashMap<>();
            for (IRInstruction.OpCode opCode : IRInstruction.OpCode.values())
                instructionCounts.put(opCode, 0);
        }

        public void update(IRInstruction instruction) {
            totalInstructionCount += 1;
            int current = instructionCounts.get(instruction.opCode);
            instructionCounts.put(instruction.opCode, current + 1);
        }

        public int getNonLabelInstructionCount() {
            return totalInstructionCount - instructionCounts.get(IRInstruction.OpCode.LABEL);
        }
    }

    // Program information
    private IRProgram program;
    private Map<String, IRFunction> functionMap;
    private Map<IRFunction, Map<String, Integer>> functionLabelMap;

    // Execution state
    private Stack<StackFrame> stack;
    private ProgramCounter pc;
    private Map<String, Integer> currentLabelMap;

    private Scanner stdinScanner;

    private Stats stats;

    public IRInterpreter(String filename) throws FileNotFoundException, IRException {
        IRReader irReader = new IRReader();
        program = irReader.parseIRFile(filename);
        initProgram();
    }

    private void initProgram() {
        functionMap = new HashMap<>();
        functionLabelMap = new HashMap<>();
        for (IRFunction function : program.functions) {
            functionMap.put(function.name, function);

            // For better efficiency
            if (!(function.instructions instanceof ArrayList))
                function.instructions = new ArrayList<>(function.instructions);

            Map<String, Integer> labelMap = new HashMap<>();
            for (int i = 0; i < function.instructions.size(); i++) {
                IRInstruction instruction = function.instructions.get(i);
                if (instruction.opCode == IRInstruction.OpCode.LABEL)
                    labelMap.put(((IRLabelOperand) instruction.operands[0]).getName(), i);
            }
            functionLabelMap.put(function, labelMap);
        }
    }

    public void run() throws IRException {
        // Add an entry call to main
        IRFunctionOperand mainFunctionOperand = new IRFunctionOperand("main", null);
        IRInstruction entryCall = new IRInstruction(IRInstruction.OpCode.CALL, new IROperand[]{mainFunctionOperand}, -1);
        ArrayList<IRInstruction> entryInstList = new ArrayList<>();
        entryInstList.add(entryCall);
        pc = new ProgramCounter();
        pc.set(entryInstList, 0);

        stack = new Stack<>();
        StackFrame entrySF = new StackFrame();
        stack.push(entrySF);

        // Do not count the entry call
        stats = new Stats();
        stats.totalInstructionCount = -1;
        stats.instructionCounts.put(IRInstruction.OpCode.CALL, -1);

        stdinScanner = new Scanner(System.in);

        while (true) {
            IRInstruction instruction = pc.next();
            executeInstruction(instruction);

            if (!pc.hasNext()) {
                // Return from a procedure
                StackFrame sf = stack.pop();
                if (stack.peek() == entrySF) // Exit main
                    break;
                IRFunction caller = sf.caller;

                if (caller.returnType != null)
                    throwRuntimeException(
                            caller.instructions.get(caller.instructions.size() - 1),
                            "Missing return for a function with return value");

                pc.set((ArrayList<IRInstruction>) caller.instructions, sf.returnInstIdx);
                currentLabelMap = functionLabelMap.get(caller);
            }
        }

        stdinScanner.close();
    }

    public Stats getStats() {
        return stats;
    }

    private Object getConstVal(IRConstantOperand constOperand) {
        if (constOperand.type == IRIntType.get())
            return Integer.parseInt(constOperand.getValueString());
        return Float.parseFloat(constOperand.getValueString());
    }

    private Object getValFromVarOrConst(IROperand operand, StackFrame sf) {
        if (operand instanceof IRVariableOperand)
            return sf.getVal((IRVariableOperand) operand);
        return getConstVal((IRConstantOperand) operand);
    }

    private Object binaryOperation(IRInstruction.OpCode opCode, IRType type, Object y, Object z) {
        if (type == IRIntType.get()) {
            int iy = (Integer) y;
            int iz = (Integer) z;
            switch (opCode) {
                case ADD:
                    return iy + iz;
                case SUB:
                    return iy - iz;
                case MULT:
                    return iy * iz;
                case DIV:
                    return iy / iz;
                case AND:
                    return iy & iz;
                case OR:
                    return iy | iz;

                case BREQ:
                    return iy == iz;
                case BRNEQ:
                    return iy != iz;
                case BRLT:
                    return iy < iz;
                case BRGT:
                    return iy > iz;
                case BRGEQ:
                    return iy >= iz;

                default:
                    assert false;
            }
        } else {
            float fy = (Float) y;
            float fz = (Float) z;
            switch (opCode) {
                case ADD:
                    return fy + fz;
                case SUB:
                    return fy - fz;
                case MULT:
                    return fy * fz;
                case DIV:
                    return fy / fz;

                case BREQ:
                    return fy == fz;
                case BRNEQ:
                    return fy != fz;
                case BRLT:
                    return fy < fz;
                case BRGT:
                    return fy > fz;
                case BRGEQ:
                    return fy >= fz;

                default:
                    assert false;
            }
        }
        return null;
    }

    private void executeInstruction(IRInstruction instruction) throws IRException {
        stats.update(instruction);

        StackFrame sf = stack.peek();
        switch (instruction.opCode) {
            case ASSIGN: {
                if (instruction.operands.length > 2) { // Array assignment
                    Object[] arr = (Object[]) getValFromVarOrConst(instruction.operands[0], sf);
                    int assignSize = (Integer) getValFromVarOrConst(instruction.operands[1], sf);
                    Object src = getValFromVarOrConst(instruction.operands[2], sf);
                    if (assignSize < 0 || assignSize > arr.length)
                        throwRuntimeException(instruction, "Out-of-bounds array access");
                    for (int i = 0; i < assignSize; i++)
                        arr[i] = src;
                } else {
                    IRVariableOperand dest = (IRVariableOperand) instruction.operands[0];
                    Object src = getValFromVarOrConst(instruction.operands[1], sf);
                    sf.setVal(dest, src);
                }
                break;
            }
            case ADD:
            case SUB:
            case MULT:
            case DIV:
            case AND:
            case OR: {
                IRVariableOperand dest = (IRVariableOperand) instruction.operands[0];
                Object y = getValFromVarOrConst(instruction.operands[1], sf);
                Object z = getValFromVarOrConst(instruction.operands[2], sf);
                Object x = binaryOperation(instruction.opCode, dest.type, y, z);
                sf.setVal(dest, x);
                break;
            }
            case GOTO: {
                int targetIdx = currentLabelMap.get(((IRLabelOperand) instruction.operands[0]).getName());
                pc.setNextIdx(targetIdx);
                break;
            }
            case BREQ:
            case BRNEQ:
            case BRLT:
            case BRGT:
            case BRGEQ: {
                int targetIdx = currentLabelMap.get(((IRLabelOperand) instruction.operands[0]).getName());
                Object a = getValFromVarOrConst(instruction.operands[1], sf);
                Object b = getValFromVarOrConst(instruction.operands[2], sf);
                IRType type = ((IRVariableOperand) instruction.operands[1]).type;
                boolean result = (Boolean) binaryOperation(instruction.opCode, type, a, b);
                if (result)
                    pc.setNextIdx(targetIdx);
                break;
            }
            case RETURN: {
                Object retVal = getValFromVarOrConst(instruction.operands[0], sf);
                IRFunction caller = sf.caller;
                IRInstruction callInst = sf.callInst;
                assert callInst.opCode == IRInstruction.OpCode.CALLR;
                stack.pop();
                StackFrame callerSF = stack.peek();
                IRVariableOperand retVar = (IRVariableOperand) callInst.operands[0];
                callerSF.setVal(retVar, retVal);
                pc.set((ArrayList<IRInstruction>) caller.instructions, sf.returnInstIdx);
                currentLabelMap = functionLabelMap.get(caller);
                break;
            }
            case CALL: {
                ArrayList<Object> arguments = new ArrayList<>();
                for (int i = 1; i < instruction.operands.length; i++)
                    arguments.add(getValFromVarOrConst(instruction.operands[i], sf));
                String calleeName = ((IRFunctionOperand) instruction.operands[0]).getName();
                if (functionMap.containsKey(calleeName))
                    executeCall(instruction, functionMap.get(calleeName), arguments);
                else
                    handleIntrinsicFunction(instruction, calleeName, arguments);
                break;
            }
            case CALLR: {
                ArrayList<Object> arguments = new ArrayList<>();
                for (int i = 2; i < instruction.operands.length; i++)
                    arguments.add(getValFromVarOrConst(instruction.operands[i], sf));
                String calleeName = ((IRFunctionOperand) instruction.operands[1]).getName();
                if (functionMap.containsKey(calleeName))
                    executeCall(instruction, functionMap.get(calleeName), arguments);
                else
                    handleIntrinsicFunction(instruction, calleeName, arguments);
                break;
            }
            case ARRAY_STORE: {
                Object val = getValFromVarOrConst(instruction.operands[0], sf);
                Object[] arr = (Object[]) getValFromVarOrConst(instruction.operands[1], sf);
                int offset = (Integer) getValFromVarOrConst(instruction.operands[2], sf);
                if (offset < 0 || offset >= arr.length)
                    throwRuntimeException(instruction, "Out-of-bounds array access");
                arr[offset] = val;
                break;
            }
            case ARRAY_LOAD: {
                IRVariableOperand dest = (IRVariableOperand) instruction.operands[0];
                Object[] arr = (Object[]) getValFromVarOrConst(instruction.operands[1], sf);
                int offset = (Integer) getValFromVarOrConst(instruction.operands[2], sf);
                if (offset < 0 || offset >= arr.length)
                    throwRuntimeException(instruction, "Out-of-bounds array access");
                sf.setVal(dest, arr[offset]);
                break;
            }
            case LABEL:
                break;
            default:
                assert false;
        }
    }

    private void executeCall(IRInstruction callInst, IRFunction function, ArrayList<Object> arguments) {
        StackFrame sf = stack.peek();
        StackFrame calleeSF = new StackFrame();
        calleeSF.caller = sf.function;
        calleeSF.callInst = callInst;
        calleeSF.returnInstIdx = pc.getNextIdx();
        calleeSF.function = function;
        calleeSF.varMap = buildVarMap(function, arguments);
        stack.push(calleeSF);
        pc.set((ArrayList<IRInstruction>) function.instructions, 0);
        currentLabelMap = functionLabelMap.get(function);
    }

    private Map<String, Object> buildVarMap(IRFunction function, ArrayList<Object> arguments) {
        Map<String, Object> varMap = new HashMap<>();

        for (IRVariableOperand variable: function.variables) {
            if (variable.type instanceof IRArrayType) {
                IRArrayType arrayType = (IRArrayType) variable.type;
                Object[] arr = new Object[arrayType.getSize()];
                varMap.put(variable.getName(), arr);
                if (arrayType.getElementType() == IRIntType.get())
                    for (int i = 0; i < arr.length; i++)
                        arr[i] = new Integer(0);
                else
                    for (int i = 0; i < arr.length; i++)
                        arr[i] = new Float(0);
            } else {
                if (variable.type == IRIntType.get())
                    varMap.put(variable.getName(), new Integer(0));
                else
                    varMap.put(variable.getName(), new Float(0));
            }
        }

        Iterator<IRVariableOperand> pit = function.parameters.iterator();
        Iterator<Object> ait = arguments.iterator();
        while (pit.hasNext()) {
            IRVariableOperand param = pit.next();
            Object arg = ait.next();
            varMap.put(param.getName(), arg);
        }

        return varMap;
    }

    private void handleIntrinsicFunction(IRInstruction callInst, String functionName, ArrayList<Object> arguments)
            throws IRException {
        switch (functionName) {
            case "geti": {
                int i;
                try {
                    i = stdinScanner.nextInt();
                } catch (InputMismatchException e) {
                    i = 0;
                }
                stdinScanner.nextLine();
                IRVariableOperand retVar = (IRVariableOperand) callInst.operands[0];
                stack.peek().setVal(retVar, i);
                break;
            }
            case "getf": {
                float f;
                try {
                    f = stdinScanner.nextFloat();
                } catch (InputMismatchException e) {
                    f = 0;
                }
                stdinScanner.nextLine();
                IRVariableOperand retVar = (IRVariableOperand) callInst.operands[0];
                stack.peek().setVal(retVar, f);
                break;
            }
            case "getc": {
                int c;
                try {
                    c = (int) System.in.read();
                } catch (IOException e) {
                    c = 0;
                }
                IRVariableOperand retVar = (IRVariableOperand) callInst.operands[0];
                stack.peek().setVal(retVar, c);
                break;
            }
            case "puti": {
                System.out.print((Integer) arguments.get(0));
                break;
            }
            case "putf": {
                System.out.print((Float) arguments.get(0));
                break;
            }
            case "putc": {
                int c = (Integer) arguments.get(0);
                System.out.print((char) c);
                break;
            }
            default:
                throwRuntimeException(callInst, String.format("Undefined reference to function '%s'", functionName));
        }
    }

    private void throwRuntimeException(IRInstruction instruction, String message) throws IRException {
        System.err.println("IR interpreter runtime exception: " + message);
        System.err.println("Stack trace:");
        ListIterator<StackFrame> sit = stack.listIterator(stack.size());
        System.err.println("\t" + stack.peek().function.name + ":" + instruction.irLineNumber);
        while (sit.hasPrevious()) {
            StackFrame sf = sit.previous();
            if (sf.caller == null)
                break;
            System.err.println("\t" + sf.caller.name + ":" + sf.caller.instructions.get(sf.returnInstIdx - 1).irLineNumber);
        }
        throw new IRException();
    }
}

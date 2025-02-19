package ir;

import ir.datatype.IRArrayType;
import ir.datatype.IRFloatType;
import ir.datatype.IRIntType;
import ir.datatype.IRType;
import ir.operand.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRReader {

    private class IRLine {
        public int lineNumber;
        public String line;

        public IRLine(int lineNumber, String line) {
            this.lineNumber = lineNumber;
            this.line = line;
        }
    }

    /**
     * A text scanner that returns a non-empty line with its line number in each iteration
     */
    private class IRScanner implements Iterator<IRLine> {
        private Scanner scanner;
        private IRLine nextLine;
        private int lineNumber;

        public IRScanner(File file) throws FileNotFoundException {
            scanner = new Scanner(file);
            lineNumber = 0;
        }

        @Override
        public boolean hasNext() {
            if (nextLine != null)
                return true;
            while (scanner.hasNextLine()) {
                lineNumber++;
                String line = scanner.nextLine().trim();
                // Ignore empty lines
                if (!line.isEmpty()) {
                    nextLine = new IRLine(lineNumber, line);
                    return true;
                }
            }
            return false;
        }

        @Override
        public IRLine next() throws NoSuchElementException {
            if (hasNext()) {
                IRLine l = nextLine;
                nextLine = null;
                return l;
            }
            throw new NoSuchElementException();
        }
    }

    private Map<String, IRFunction> intrinsics;

    public IRReader() {
        intrinsics = new HashMap<>();
        intrinsics.put("geti", new IRFunction("geti", IRIntType.get(), new ArrayList<>(), null, null));
        intrinsics.put("getf", new IRFunction("getf", IRFloatType.get(), new ArrayList<>(), null, null));
        intrinsics.put("getc", new IRFunction("getc", IRIntType.get(), new ArrayList<>(), null, null));
        intrinsics.put("puti", new IRFunction("puti", null, Arrays.asList(new IRVariableOperand(IRIntType.get(), "i", null)), null, null));
        intrinsics.put("putf", new IRFunction("putf", null, Arrays.asList(new IRVariableOperand(IRFloatType.get(), "f", null)), null, null));
        intrinsics.put("putc", new IRFunction("putc", null, Arrays.asList(new IRVariableOperand(IRIntType.get(), "c", null)), null, null));
    }

    public IRProgram parseIRFile(String filename) throws FileNotFoundException, IRException {
        Set<String> functionNames = new HashSet<>();
        List<IRFunction> functions = new ArrayList<>();

        IRScanner irScanner = new IRScanner(new File(filename));
        List<IRLine> irLines = new ArrayList<>();
        while (irScanner.hasNext()) {
            IRLine irLine = irScanner.next();
            String line = irLine.line;
            int lineNumber = irLine.lineNumber;

            if (line.startsWith("#start_function")) {
                if (!irLines.isEmpty())
                    throw new IRException("Unexpected #start_function", lineNumber);
                irLines.add(irLine);
            } else if (line.startsWith("#end_function")) {
                if (irLines.isEmpty())
                    throw new IRException("Unexpected #end_function", lineNumber);
                irLines.add(irLine);

                IRFunction f = parseFunction(irLines);
                if (functionNames.contains(f.name))
                    throw new IRException(
                            String.format("Redefinition of function '%s'", f.name),
                            irLines.get(0).lineNumber);
                if (intrinsics.containsKey(f.name))
                    throw new IRException(
                            String.format("Redefinition of intrinsic function '%s'", f.name),
                            irLines.get(0).lineNumber);

                functions.add(f);
                functionNames.add(f.name);
                irLines.clear();
            } else {
                irLines.add(irLine);
            }
        }

        // Check calls
        Map<String, IRFunction> functionMap = new HashMap<>();
        for (IRFunction f : functions)
            functionMap.put(f.name, f);
        for (IRFunction f : intrinsics.values())
            functionMap.put(f.name, f);
        for (IRFunction f : functions)
            for (IRInstruction inst : f.instructions) {
                IRFunction callee;
                int argIdx;
                if (inst.opCode == IRInstruction.OpCode.CALL) {
                    callee = functionMap.get(((IRFunctionOperand) inst.operands[0]).getName());
                    if (callee == null || callee.parameters.size() != inst.operands.length - 1 || callee.returnType != null)
                        throw new IRException("Invalid function call", inst.irLineNumber);
                    argIdx = 1;
                } else if (inst.opCode == IRInstruction.OpCode.CALLR) {
                    callee = functionMap.get(((IRFunctionOperand) inst.operands[1]).getName());
                    IRVariableOperand retVar = (IRVariableOperand) inst.operands[0];
                    if (callee == null || callee.parameters.size() != inst.operands.length - 2 || retVar.type != callee.returnType)
                        throw new IRException("Invalid function call", inst.irLineNumber);
                    argIdx = 2;
                } else
                    continue;
                for (IRVariableOperand param : callee.parameters) {
                    IROperand arg = inst.operands[argIdx];
                    IRType argType;
                    if (arg instanceof IRVariableOperand)
                        argType = ((IRVariableOperand) arg).type;
                    else
                        argType = ((IRConstantOperand) arg).type;
                    if (param.type != argType)
                        throw new IRException("Invalid function call", inst.irLineNumber);
                    argIdx++;
                }
            }

        return new IRProgram(functions);
    }

    private Pattern typePattern = Pattern.compile("^(?:(void)|(?:(int|float)(?:\\[(\\d+)\\])?))$");

    private IRType parseType(String typeStr, int lineNumber) throws IRException {
        Matcher m = typePattern.matcher(typeStr);
        if (!m.matches())
            throw new IRException(String.format("Invalid type '%s'", typeStr), lineNumber);
        if (m.group(1) != null)
            return null;
        IRType elementType;
        switch (m.group(2)) {
            case "int":
                elementType = IRIntType.get();
                break;
            case "float":
                elementType = IRFloatType.get();
                break;
            default:
                throw new IRException(String.format("Invalid type '%s'", typeStr), lineNumber);
        }
        if (m.group(3) == null)
            return elementType;
        int size = Integer.parseInt(m.group(3));
        if (size <= 0)
            throw new IRException(String.format("Invalid array size '%d'", size), lineNumber);
        return IRArrayType.get(elementType, size);
    }

    private IRFunction parseFunction(List<IRLine> irLines)
            throws IRException {
        // variable name -> IRVariableOperand
        Map<String, IRVariableOperand> variableMap = new HashMap<>();

        Iterator<IRLine> it = irLines.iterator();
        it.next(); // Skip #start_function

        // Start parsing signature
        IRLine signatureLine = it.next();
        String[] signatureTokens = signatureLine.line.replaceAll("[\\s(),:]+", " ").split(" ");
        if (signatureTokens.length < 2 || signatureTokens.length % 2 != 0)
            throw new IRException("Invalid function signature", signatureLine.lineNumber);

        // Get return type
        IRType retType = parseType(signatureTokens[0], signatureLine.lineNumber);
        if (retType instanceof IRArrayType)
            throw new IRException(String.format("Invalid type '%s'", signatureTokens[0]), signatureLine.lineNumber);

        // Get function name
        String functionName = signatureTokens[1];

        // Get parameters
        List<IRVariableOperand> params = new ArrayList<>();
        for (int i = 2; i < signatureTokens.length; i += 2) {
            IRType paramType = parseType(signatureTokens[i], signatureLine.lineNumber);
            if (paramType == null)
                throw new IRException(String.format("Invalid type '%s'", signatureTokens[i]), signatureLine.lineNumber);
            String paramName = signatureTokens[i + 1];
            if (!paramName.matches("^[A-Za-z_][A-Za-z0-9_]*$"))
                throw new IRException(String.format("Invalid parameter name '%s'", paramName), signatureLine.lineNumber);
            if (variableMap.containsKey(paramName))
                throw new IRException(
                        String.format("Redefinition of variable '%s'", paramName),
                        signatureLine.lineNumber);
            IRVariableOperand param = new IRVariableOperand(paramType, paramName, null);
            variableMap.put(paramName, param);
            params.add(param);
        }

        // Parse variable lists
        IRLine intListLine = it.next();
        IRLine floatListLine = it.next();
        parseVariableList(intListLine, IRIntType.get(), variableMap);
        parseVariableList(floatListLine, IRFloatType.get(), variableMap);

        // Parse instructions
        List<IRInstruction> instructions = new ArrayList<>();
        while (it.hasNext()) {
            IRLine irLine = it.next();
            if (irLine.line.startsWith("#")) // Ignore #end_function
                break;
            instructions.add(parseInstruction(irLine, variableMap));
        }

        // Check Labels
        Set<String> labels = new HashSet<>();
        for (IRInstruction inst : instructions)
            if (inst.opCode == IRInstruction.OpCode.LABEL) {
                String label = ((IRLabelOperand) inst.operands[0]).getName();
                if (!labels.add(label))
                    throw new IRException(String.format("Redefinition of label '%s'", label), inst.irLineNumber);
            }
        for (IRInstruction inst : instructions)
            if (inst.opCode == IRInstruction.OpCode.GOTO || inst.opCode.toString().startsWith("br")) {
                String label = ((IRLabelOperand) inst.operands[0]).getName();
                if (!labels.contains(label))
                    throw new IRException(String.format("Undefined reference to label '%s'", label), inst.irLineNumber);
            }

        // Check return
        for (IRInstruction inst : instructions)
            if (inst.opCode == IRInstruction.OpCode.RETURN) {
                if (retType == null)
                    throw new IRException("Return instruction is not allowed in this function", inst.irLineNumber);
                IRType type = getDataType(inst.operands[0]);
                if (type != retType)
                    throw new IRException("Invalid return value type", inst.irLineNumber);
            }

        // Check the main function
        if (functionName.equals("main")) {
            if (retType != null || params.size() != 0)
                throw new IRException("Invalid main function", signatureLine.lineNumber);
        }

        return new IRFunction(functionName, retType, params, new ArrayList<>(variableMap.values()), instructions);
    }

    private Pattern arrayPattern = Pattern.compile("^(.+)\\[(\\d+)\\]$");

    private Pattern varNamePattern = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private void parseVariableList(IRLine varListLine, IRType elementType,
                                   Map<String, IRVariableOperand> variableMap) throws IRException {
        String varListStr = varListLine.line;
        varListStr = varListStr.substring(varListStr.indexOf(':') + 1).trim();
        if (varListStr.isEmpty())
            return;
        String[] varNames = varListStr.split("[\\s,]+");
        for (String varName : varNames) {
            IRVariableOperand irVar;
            Matcher m = arrayPattern.matcher(varName);
            if (m.matches()) { // Array
                int size = Integer.parseInt(m.group(2));
                if (size <= 0)
                    throw new IRException(String.format("Invalid array size '%d'", size), varListLine.lineNumber);
                IRType arrayType = IRArrayType.get(elementType, size);
                String arrayName = m.group(1);
                if (!varNamePattern.matcher(arrayName).matches())
                    throw new IRException(String.format("Invalid variable name '%s'", arrayName), varListLine.lineNumber);
                irVar = new IRVariableOperand(arrayType, arrayName, null);
            } else {
                if (!varNamePattern.matcher(varName).matches())
                    throw new IRException(String.format("Invalid variable name '%s'", varName), varListLine.lineNumber);
                irVar = new IRVariableOperand(elementType, varName, null);
            }
            if (variableMap.containsKey(irVar.getName()))
                throw new IRException(
                        String.format("Redefinition of variable '%s'", irVar.getName()),
                        varListLine.lineNumber);
            variableMap.put(irVar.getName(), irVar);
        }
    }

    private IRInstruction parseInstruction(IRLine irLine,
                                           Map<String, IRVariableOperand> variableMap) throws IRException {
        String line = irLine.line;
        int lineNumber = irLine.lineNumber;

        IRInstruction instruction = new IRInstruction();
        instruction.irLineNumber = lineNumber;

        // Treat label as a pseudo instruction
        if (line.endsWith(":")) {
            String labelStr = line.substring(0, line.length() - 1);
            IRLabelOperand labelOperand = new IRLabelOperand(labelStr, instruction);
            instruction.opCode = IRInstruction.OpCode.LABEL;
            instruction.operands = new IROperand[]{labelOperand};
            return instruction;
        }

        // Parse opcode
        String[] tokens = line.split("[,\\s]+");
        IRInstruction.OpCode opCode;
        try {
            opCode = IRInstruction.OpCode.valueOf(tokens[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IRException(String.format("Invalid OpCode '%s'", tokens[0]), lineNumber);
        }
        instruction.opCode = opCode;

        // Parse operands
        List<IROperand> operands = new ArrayList<>();
        switch (opCode) {
            case ASSIGN: {
                if (tokens.length > 3) {
                    getConstantOrVariableOperands(instruction, operands, tokens, 1, 3, variableMap, lineNumber);
                    IRType t0 = getDataType(operands.get(0));
                    IRType t1 = getDataType(operands.get(1));
                    IRType t2 = getDataType(operands.get(2));
                    if (!(t0 instanceof IRArrayType
                            && t1 instanceof IRIntType
                            && ((IRArrayType) t0).getElementType() == t2))
                        throw new IRException("Invalid operand", instruction.irLineNumber);
                } else {
                    getConstantOrVariableOperands(instruction, operands, tokens, 1, 2, variableMap, lineNumber);
                    IRType t0 = getDataType(operands.get(0));
                    IRType t1 = getDataType(operands.get(1));
                    if (t0 instanceof IRArrayType || t0 != t1)
                        throw new IRException("Invalid operand", instruction.irLineNumber);
                }
                break;
            }
            case ADD:
            case SUB:
            case MULT:
            case DIV:
            case AND:
            case OR: {
                getConstantOrVariableOperands(instruction, operands, tokens, 1, 3, variableMap, lineNumber);
                IRType t0 = getDataType(operands.get(0));
                IRType t1 = getDataType(operands.get(1));
                IRType t2 = getDataType(operands.get(2));
                if (!(operands.get(0) instanceof IRVariableOperand) ||
                        t0 instanceof IRArrayType || t0 != t1 || t1 != t2)
                    throw new IRException("Invalid operand", instruction.irLineNumber);
                break;
            }
            case GOTO: {
                operands.add(new IRLabelOperand(tokens[1], instruction));
                break;
            }
            case BREQ:
            case BRNEQ:
            case BRLT:
            case BRGT:
            case BRGEQ: {
                operands.add(new IRLabelOperand(tokens[1], instruction));
                getConstantOrVariableOperands(instruction, operands, tokens, 2, 3, variableMap, lineNumber);
                IRType t1 = getDataType(operands.get(1));
                IRType t2 = getDataType(operands.get(2));
                if (t1 instanceof IRArrayType || t1 != t2)
                    throw new IRException("Invalid operand", instruction.irLineNumber);
                break;
            }
            case RETURN: {
                getConstantOrVariableOperands(instruction, operands, tokens, 1, 1, variableMap, lineNumber);
                IRType t0 = getDataType(operands.get(0));
                if (t0 instanceof IRArrayType)
                    throw new IRException("Invalid operand", instruction.irLineNumber);
                break;
            }
            case CALL: {
                operands.add(new IRFunctionOperand(tokens[1], instruction));
                getConstantOrVariableOperands(instruction, operands, tokens, 2, tokens.length - 1, variableMap, lineNumber);
                break;
            }
            case CALLR: {
                getConstantOrVariableOperands(instruction, operands, tokens, 1, 1, variableMap, lineNumber);
                operands.add(new IRFunctionOperand(tokens[2], instruction));
                getConstantOrVariableOperands(instruction, operands, tokens, 3, tokens.length - 1, variableMap, lineNumber);
                IRType t0 = getDataType(operands.get(0));
                if (!(operands.get(0) instanceof IRVariableOperand) || t0 instanceof IRArrayType)
                    throw new IRException("Invalid operand", instruction.irLineNumber);
                break;
            }
            case ARRAY_STORE: {
                getConstantOrVariableOperands(instruction, operands, tokens, 1, 3, variableMap, lineNumber);
                IRType t0 = getDataType(operands.get(0));
                IRType t1 = getDataType(operands.get(1));
                IRType t2 = getDataType(operands.get(2));
                if (!(!(t0 instanceof IRArrayType)
                        && t1 instanceof IRArrayType && t2 instanceof IRIntType
                        && ((IRArrayType) t1).getElementType() == t0))
                    throw new IRException("Invalid operand", instruction.irLineNumber);
                break;
            }
            case ARRAY_LOAD: {
                getConstantOrVariableOperands(instruction, operands, tokens, 1, 3, variableMap, lineNumber);
                IRType t0 = getDataType(operands.get(0));
                IRType t1 = getDataType(operands.get(1));
                IRType t2 = getDataType(operands.get(2));
                if (!(operands.get(0) instanceof IRVariableOperand && !(t0 instanceof IRArrayType)
                        && t1 instanceof IRArrayType && t2 instanceof IRIntType
                        && ((IRArrayType) t1).getElementType() == t0))
                    throw new IRException("Invalid operand", instruction.irLineNumber);
                break;
            }
            default:
                throw new IRException("Invalid OpCode", lineNumber);
        }
        instruction.operands = operands.toArray(new IROperand[0]);

        return instruction;
    }

    private void getConstantOrVariableOperands(IRInstruction instruction,
                                               List<IROperand> operands,
                                               String[] tokens, int startIdx, int endIdx,
                                               Map<String, IRVariableOperand> variableMap,
                                               int lineNumber) throws IRException {
        for (int i = startIdx; i <= endIdx; i++) {
            String token = tokens[i];
            if (isConstant(token)) {
                if (token.indexOf('.') >= 0)
                    operands.add(new IRConstantOperand(IRFloatType.get(), token, instruction));
                else
                    operands.add(new IRConstantOperand(IRIntType.get(), token, instruction));
            } else {
                if (!variableMap.containsKey(token))
                    throw new IRException(String.format("Variable '%s' used without definition", token), lineNumber);
                IRVariableOperand variable = variableMap.get(token);
                operands.add(new IRVariableOperand(variable.type, variable.getName(), instruction));
            }
        }
    }

    private boolean isConstant(String s) {
        return s.matches("^-?\\d+(\\.\\d*)?$");
    }

    private IRType getDataType(IROperand x) {
        if (x instanceof IRConstantOperand)
            return ((IRConstantOperand) x).type;
        if (x instanceof IRVariableOperand)
            return ((IRVariableOperand) x).type;
        return null;
    }

}

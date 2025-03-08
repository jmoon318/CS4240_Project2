import ir.IRFunction;
import ir.IRInstruction;
import ir.IRProgram;
import ir.datatype.IRArrayType;
import ir.datatype.IRIntType;
import ir.operand.IROperand;
import ir.operand.IRVariableOperand;

import java.io.PrintStream;
import java.util.*;

public class GreedyAllocator {

    private static final Map<IRInstruction.OpCode, String> opCodeToAssembly = new HashMap<>();

    private PrintStream ps;

    public GreedyAllocator(PrintStream ps) {
        this.ps = ps;
    }

    private Map<String, Integer> operandToRegisterMap = null;
    String t9Name;
    String t8Name;
    String t7Name;

    public void GreedyPrintBlock(BasicBlock block) {
        if (operandToRegisterMap.size() > 10) {
            for (String op : operandToRegisterMap.keySet()) {
                if (operandToRegisterMap.get(op) == 9) {
                    this.t9Name = op;
                }
                if (operandToRegisterMap.get(op) == 9) {
                    this.t8Name = op;
                }
                if (operandToRegisterMap.get(op) == 9) {
                    this.t7Name = op;
                }
            }
        }


        InterferenceGraph iGraph = block.getIGraph();
        operandToRegisterMap = iGraph.getRegMap();
        // IR-ASM mapping

        for (IRInstruction instr : block.instructions) {
            printInstruction(instr);
        }
    }

    public void printFunction(IRFunction function) {


        ps.print(function.name);
        ps.println(':');

        // Print instructions
        for (IRInstruction instruction : function.instructions) {
            printInstruction(instruction);
        }
        // ps.println("    # Exit program");
        // ps.println("    li $v0, 10");
        // ps.println("    syscall");
    }

    public void printInstruction(IRInstruction instruction) {
        if (instruction.opCode == IRInstruction.OpCode.LABEL) {
            ps.print(instruction.operands[0]);
            ps.print(":");
            ps.println();
            return;
        }


        String op = instruction.opCode.toString();
        IROperand[] operands = instruction.operands;

        switch (op) {
            case "assign":
                // assign, dest, value → li dest, value
                String dest = operands[0].getValue();
                String value = operands[1].getValue();
                
                    // only need li if this vregister does not spill
                    // if it does spill then old t9 should be saved and then restored once this is complete

                    if (isNumeric(value)) {
                        if (getRegister(dest).equals("")) {
                            // spill the destination
                            ps.println("    li $t9, " + value);
                            ps.println("    sw $t9, 0(" + dest + ")");
                        } else {
                            ps.println("    li " + getRegister(dest) + ", " + value);
                        }
                    } else {
                        if (getRegister(dest).equals("") && getRegister(value).equals("")) {
                            // spill source and destination
                            ps.println("    lw $t9, 0(" + value + ")");
                            ps.println("    sw $t9, 0(" + dest + ")");
                        } else if (getRegister(dest).equals("")) {
                            // spill dest
                            ps.println("    sw " + value + ", 0(" + dest + ")");
                        } else if (getRegister(value).equals("")) {
                            // spill source
                            ps.println("    lw $t9, 0(" + value + ")");
                            ps.println("    move $t9, " + dest);
                        } else {
                            ps.println("    move " + dest + ", " + value);
                        }
                    }
                break;

            case "add":
            case "sub":
            case "mult":
            case "div":
            case "and":
            case "or":

                String res = operands[0].getValue();
                String op1 = operands[1].getValue();
                String op2 = operands[2].getValue();

                if (isNumeric(op2)) {
                    if (getRegister(res).equals("") && getRegister(op1).equals("")) {
                        ps.println("    lw $t8, 0(" + op1 + ")");
                        ps.println("    " + op + "i " + "$t9, $t8, " + op2);
                        ps.println("    sw $t9, 0(" + res + ")");
                    } else if (getRegister(res).equals("")) {
                        ps.println("    " + op + "i " + "$t9, " + op1 + ", " + op2);
                        ps.println("    sw $t9, 0(" + res + ")");
                    } else if (getRegister(op1).equals("")) {
                        ps.println("    lw $t9, 0(" + op1 + ")");
                        ps.println("    " + op + "i " + res + ", $t9, " + op2);
                    } else {
                        ps.println("    " + op + "i " + res + ", " + op1 + ", " + op2);
                    }
                } else {
                    if (getRegister(res).equals("") && getRegister(op1).equals("") && getRegister(op2).equals("")) {
                        ps.println("    lw $t8, 0(" + op1 + ")");
                        ps.println("    lw $t7, 0(" + op2 + ")");
                        ps.println("    " + op + " $t9, $t8, $t7");
                        ps.println("    sw $t9, 0(" + res + ")");
                    } else if (getRegister(res).equals("") && getRegister(op1).equals("")) {
                        ps.println("    lw $t8, 0(" + op1 + ")");
                        ps.println("    " + op + " $t9, $t8, " + op2);
                        ps.println("    sw $t9, 0(" + res + ")");
                    } else if (getRegister(op1).equals("") && getRegister(op2).equals("")) {
                        ps.println("    lw $t8, 0(" + op1 + ")");
                        ps.println("    lw $t7, 0(" + op2 + ")");
                        ps.println("    " + op + " " + res + ", $t8, $t7");
                    } else if (getRegister(res).equals("") && getRegister(op2).equals("")) {
                        ps.println("    lw $t7, 0(" + op2 + ")");
                        ps.println("    " + op + " $t9, " + op1 + ", $t7");
                        ps.println("    sw $t9, 0(" + res + ")");
                    } else if (getRegister(res).equals("")) {
                        ps.println("    " + op + " $t9, " + op1 + ", " + op2);
                        ps.println("    sw $t9, 0(" + res + ")");
                    } else if (getRegister(op1).equals("")) {
                        ps.println("    lw $t8, 0(" + op1 + ")");
                        ps.println("    " + op + " " + res + ", $t8, " + op2);
                    } else if (getRegister(op2).equals("")) {
                        ps.println("    lw $t7, 0(" + op2 + ")");
                        ps.println("    " + op + " " + res + ", " + op1 + ", $t7");
                    } else {
                        ps.println("    " + op + " " + res + ", " + op1 + ", " + op2);
                    }
                }
                break;

            case "goto":
                ps.println("    j " + operands[0].getValue());
                break;

            case "breq":  // Branch if Equal (==)
            case "brneq": // Branch if Not Equal (!=)
            case "brlt":  // Branch if Less Than (<)
            case "brgt":  // Branch if Greater Than (>)
            case "brgeq": // Branch if Greater Than or Equal (>=)
            case "brleq": // Branch if Less Than or Equal (<=)

                String lbl = operands[0].getValue();
                String cmp1 = operands[1].getValue();
                String cmp2 = operands[2].getValue();

                boolean cmp1Spilled = false;
                boolean cmp2Spilled = false;

                if (isNumeric(cmp1)) {
                    cmp1Spilled = true;
                    ps.println("    li $t9, " + cmp1);
                } else {
                    if (getRegister(cmp1).equals("")) {
                        cmp1Spilled = true;
                        ps.println("    lw $t9, 0(" + cmp1 + ")");
                    }
                }

                if (isNumeric(cmp2)) {
                    cmp2Spilled = true;
                    ps.println("    li $t8, " + cmp2);
                } else {
                    if (getRegister(cmp1).equals("")) {
                        cmp2Spilled = true;
                        ps.println("    lw $t8, 0(" + cmp2 + ")");
                    }
                }

                String branchOp = getMIPSBranchOp(op);
                ps.print("    " + branchOp + " ");

                if (cmp1Spilled) {
                    ps.print("$t9, ");
                } else {
                    ps.print(cmp1 + ", ");
                }

                if (cmp2Spilled) {
                    ps.print("$t8, ");
                } else {
                    ps.print(cmp2 + ", ");
                }

                ps.println(lbl);
                break;

            case "return":
                ps.println("    jr $ra");
                break;

            case "call":
            case "callr":
                String dest1 = (op.equals("callr")) ? operands[0].getValue() : null;
                String functionName = (op.equals("callr")) ? operands[1].getValue() : operands[0].getValue();
                String arg = (op.equals("callr")) ? (operands.length > 2 ? operands[2].getValue() : null)
                        : (operands.length > 1 ? operands[1].getValue() : null);

                // (`geti`) → syscall 5
                if (functionName.equals("geti")) {
                    ps.println("    li $v0, 5");
                    ps.println("    syscall");

                    if (dest1 != null) {
                        String reg = getRegister(dest1);
                        if (reg.equals("")) {
                            //ps.println("    move " + reg + ", $v0");
                            ps.println("    sw $v0, 0(" + dest1 + ")");
                        } else {
                            ps.println("    move " + reg + ", $v0");
                            //ps.println("    sw " + reg + ", " + dest1);
                        }
                    }
                }

                // (`getc`) → syscall 12**
                else if (functionName.equals("getc")) {
                    ps.println("    li $v0, 12");
                    ps.println("    syscall");

                    if (dest1 != null) {
                        String reg = getRegister(dest1);
                        if (reg.equals("")) {
                            ps.println("    sw $v0, 0(" + dest1 + ")");
                        } else {
                            ps.println("    move " + reg + ", $v0");
                        }
                    }
                }

                //  (`puti`) → syscall 1
                else if (functionName.equals("puti")) {
                    if (isNumeric(arg)) {
                        ps.println("    li $a0, " + arg);
                    } else {
                        ps.println("    lw $a0, 0(" + arg + ")");
                    }
                    ps.println("    li $v0, 1");
                    ps.println("    syscall");
                }

                // (`putc`) → syscall 11
                else if (functionName.equals("putc")) {
                    if (isNumeric(arg)) {
                        ps.println("    li $a0, " + arg);
                    } else {
                        ps.println("    lw $a0, 0(" + arg + ")");
                    }
                    ps.println("    li $v0, 11");
                    ps.println("    syscall");
                }

                /*
                *   PUT
                *   USER
                *   CALLED
                *   FUNCTIONS
                *   HERE!
                * 
                *   DON'T
                *   FORGET
                *   ABOUT
                *   SPILLING!
                */

                break;

            case "array_store":
                // array_store, arr, index, value → arr[index] = value
                String arr = operands[0].getValue();
                String index = operands[1].getValue();
                String val = operands[2].getValue();

                ps.println("    lw " + getRegister(index) + ", 0(" + index + ")");
                ps.println("    lw " + getRegister(val) + ", 0(" + val + ")");
                ps.println("    sll " + getRegister(index) + ", " + getRegister(index) + ", 2");
                ps.println("    add " + getRegister(index) + ", " + getRegister(index) + ", " + arr);
                ps.println("    sw " + getRegister(val) + ", 0(" + getRegister(index) + ")");
                break;

            case "array_load":
                // array_load, dest, arr, index → dest = arr[index]
                String loadDest = operands[0].getValue();
                String loadArr = operands[1].getValue();
                String loadIdx = operands[2].getValue();

                ps.println("    lw " + getRegister(loadIdx) + ", 0(" + loadIdx + ")");
                ps.println("    sll " + getRegister(loadIdx) + ", " + getRegister(loadIdx) + ", 2");
                
                ps.println("    add " + getRegister(loadIdx) + ", " + getRegister(loadIdx) + ", " + loadArr);
                
                ps.println("    lw " + getRegister(loadDest) + ", 0(" + getRegister(loadIdx) + ")");
                ps.println("    sw " + getRegister(loadDest) + ", 0(" + loadDest + ")");
                break;

            case "label":
                ps.println(operands[0].getValue() + ":");
                break;

            default:
                System.out.println("Unsupported IR OpCode: " + op);
        }
        ps.println();
    }

    private String getMIPSBranchOp(String irOp) {
        Map<String, String> branchMap = Map.of(
                "breq", "beq",   // if (a == b) → beq
                "brneq", "bne",  // if (a != b) → bne
                "brlt", "blt",   // if (a < b) → blt
                "brgt", "bgt",   // if (a > b) → bgt
                "brgeq", "bge",  // if (a >= b) → bge
                "brleq", "ble"   // if (a <= b) → ble
        );
        return branchMap.getOrDefault(irOp, "");
    }

    public static void IRToAssemblyConverter (){

            opCodeToAssembly.put(IRInstruction.OpCode.ASSIGN, "li");
            opCodeToAssembly.put(IRInstruction.OpCode.ADD, "add");
            opCodeToAssembly.put(IRInstruction.OpCode.SUB, "sub");
            opCodeToAssembly.put(IRInstruction.OpCode.MULT, "mul");
            opCodeToAssembly.put(IRInstruction.OpCode.DIV, "div");

            opCodeToAssembly.put(IRInstruction.OpCode.GOTO, "j");
            opCodeToAssembly.put(IRInstruction.OpCode.BREQ, "beq");
            opCodeToAssembly.put(IRInstruction.OpCode.BRNEQ, "bne");
            opCodeToAssembly.put(IRInstruction.OpCode.BRLT, "blt");
            opCodeToAssembly.put(IRInstruction.OpCode.BRGT, "bgt");
            opCodeToAssembly.put(IRInstruction.OpCode.BRGEQ, "bge");

            opCodeToAssembly.put(IRInstruction.OpCode.RETURN, "jr $ra");
            opCodeToAssembly.put(IRInstruction.OpCode.CALL, "jal");
            opCodeToAssembly.put(IRInstruction.OpCode.CALLR, "jalr");

            opCodeToAssembly.put(IRInstruction.OpCode.ARRAY_STORE, "sw");
            opCodeToAssembly.put(IRInstruction.OpCode.ARRAY_LOAD, "lw");

            opCodeToAssembly.put(IRInstruction.OpCode.LABEL, "");

    }

    public static String getAssemblyInstruction(IRInstruction.OpCode opCode) {
        return opCodeToAssembly.getOrDefault(opCode, "UNKNOWN");
    }

    public boolean isNumeric(String str) {
        return str.matches("-?\\d+");
    }


    public String getRegister(String operand) {
        int regNo = 10;
        if (operandToRegisterMap.containsKey(operand)) {
            regNo = operandToRegisterMap.get(operand);
        }
        if (regNo > 9) {
            return "";
        }

        String reg = "$t" + regNo;
        return reg;
    }
}


import ir.IRFunction;
import ir.IRInstruction;
import ir.IRProgram;
import ir.IRInstruction.OpCode;
import ir.datatype.IRArrayType;
import ir.datatype.IRIntType;
import ir.operand.IROperand;
import ir.operand.IRVariableOperand;

import java.io.PrintStream;
import java.util.*;

public class GreedyAllocator {

    private static final Map<IRInstruction.OpCode, String> opCodeToAssembly = new HashMap<>();

    private PrintStream ps;

    public GreedyAllocator(PrintStream ps, BasicBlock block, Map<String, Integer> stackMap) {
        this.ps = ps;
        this.basicBlock = block;
        this.operandToRegisterMap = block.getRegMap();
        this.stackMap = stackMap;
    }

    private Map<String, Integer> operandToRegisterMap = null;
    Map<String, Integer> stackMap = null;
    BasicBlock basicBlock = null;
    String t9Name;
    String t8Name;
    String t7Name;

    public  void printInstruction(IRInstruction instruction, IRFunction func) {
        String c_function_name = func.name;
        if (instruction.opCode == IRInstruction.OpCode.LABEL) {
            String labelName = instruction.operands[0].getValue();
            String uniqueLabel =  c_function_name + "_" + labelName;
            ps.println(uniqueLabel + ":");
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
                            ps.println("    sw $t9, -" + getOff(dest) + "($fp)");
                        } else {

                            ps.println("    li " + getRegister(dest) + ", " + value);
                        }
                    } else {
                        if (getRegister(dest).equals("") && getRegister(value).equals("")) {
                            // spill source and destination
                            ps.println("    lw $t9, -" + getOff(value) + "($fp)");
                            ps.println("    sw $t9, -" + getOff(dest) + "($fp)");
                        } else if (getRegister(dest).equals("")) {
                            // spill dest
                            ps.println("    sw " + getRegister(value) + ", -" + getOff(dest) + "($fp)");
                        } else if (getRegister(value).equals("")) {
                            // spill source
                            ps.println("    lw " + getRegister(dest) + ", -" + getOff(value) + "($fp)");
                        } else {
                            ps.println("    move " + getRegister(dest) + ", " + getRegister(value));
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
                String op1_asm = getRegister(op1);
                String op2_asm = getRegister(op2);
                String res_asm = getRegister(res);
                if (op.equals("mult")) {
                    op = "mul";
                }

                if (isNumeric(op1) && isNumeric(op2)) {
                    ps.println("    li $t8, " + op1);
                    ps.println("    li $t9, " + op2);
                    op1_asm = "$t8";
                    op1_asm = "$t9";
                } else if (isNumeric(op1)){
                    ps.println("    li $t8, " + op1);
                    op1_asm = "$t8";
                }else if (isNumeric(op2)) {
                    ps.println("    li $t9, " + op2);
                    op2_asm = "$t9";
                } 
                if (res_asm.equals("") && op1_asm.equals("") && op2_asm.equals("")) {
                    ps.println("    lw $t8, -" + getOff(op1) + "($fp)");
                    ps.println("    lw $t9, -" + getOff(op2) + "($fp)");
                    ps.println("    " + op + " $t9, $t8, $t9");
                    ps.println("    sw $t9, -" + getOff(res) + "($fp)");
                } else if (res_asm.equals("") && op2_asm.equals("")) {
                    ps.println("    lw $t8, -" + getOff(op1) + "($fp)");
                    ps.println("    " + op + " $t9, $t8, " + op2_asm);
                    ps.println("    sw $t9, -" + getOff(res) + "($fp)");
                } else if (op1_asm.equals("") && op2_asm.equals("")) {
                    ps.println("    lw $t8, -" + getOff(op1) + "($fp)");
                    ps.println("    lw $t9, -" + getOff(op2) + "($fp)");
                    ps.println("    " + op + " " + res_asm + ", $t8, $t9");
                } else if (res_asm.equals("") && op2_asm.equals("")) {
                    ps.println("    lw $t9, -" + getOff(op2) + "($fp)");
                    ps.println("    " + op + " $t9, " + op1_asm + ", $t9");
                    ps.println("    sw $t9, -" + getOff(res) + "($fp)");
                } else if (res_asm.equals("")) {
                    ps.println("    " + op + " $t9, " + op1_asm + ", " + op2_asm);
                    ps.println("    sw $t9, -" + getOff(res) + "($fp)");
                } else if (op1_asm.equals("")) {
                    ps.println("    lw $t8, -" + getOff(op1) + "($fp)");
                    ps.println("    " + op + " " + res_asm + ", $t8, " + op2_asm);
                } else if (op2_asm.equals("")) {
                    ps.println("    lw $t9, -" + getOff(op2) + "($fp)");
                    ps.println("    " + op + " " + res_asm + ", " + op1_asm + ", $t9");
                } else {
                    ps.println("    " + op + " " + res_asm + ", " + op1_asm + ", " + op2_asm);
                }
                break;

            case "goto":
                ps.println("    j " + c_function_name + "_" + operands[0].getValue());
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
                        ps.println("    lw $t9, -" + getOff(cmp1) + "($fp)");
                    }
                }

                if (isNumeric(cmp2)) {
                    cmp2Spilled = true;
                    ps.println("    li $t8, " + cmp2);
                } else {
                    if (getRegister(cmp2).equals("")) {
                        cmp2Spilled = true;
                        ps.println("    lw $t8, -" + getOff(cmp2) + "($fp)");
                    }
                }

                String branchOp = getMIPSBranchOp(op);
                ps.print("    " + branchOp + " ");

                if (cmp1Spilled) {
                    ps.print("$t9, ");
                } else {
                    ps.print(getRegister(cmp1) + ", ");
                }

                if (cmp2Spilled) {
                    ps.print("$t8, ");
                } else {
                    ps.print(getRegister(cmp2) + ", ");
                }

                ps.println(c_function_name + "_" + lbl);
                break;

            case "return":
                
                if (operands.length >= 1) {
                    if (isNumeric(operands[0].getValue())) {
                        ps.println("    li $v0, " + operands[0].getValue());
                    } else if (getRegister(operands[0].getValue()).equals("")) {
                        ps.println("    lw $v0, -" + getOff(operands[0].getValue()) + "($fp)");
                    } else {
                        ps.println("    move $v0, " + getRegister(operands[0].getValue()));
                    }
                }
                
                ps.println("    lw   $ra, 0($sp)");
                ps.println("    addi $sp, $sp, 4");
                if (func.local_size > 0) {
                    ps.println("    addi $sp, $sp, " + func.local_size);
                }
                ps.println("    lw   $fp, 0($sp)");
                ps.println("    addi $sp, $sp, 4");
                ps.println("    jr $ra");
                break;

            case "call":
            case "callr":
                String dest1 = (op.equals("callr")) ? operands[0].getValue() : null;
                String functionName = (op.equals("callr")) ? operands[1].getValue() : operands[0].getValue();
                String arg = (op.equals("callr")) ? (operands.length > 2 ? operands[2].getValue() : null)
                        : (operands.length > 1 ? operands[1].getValue() : null);

                List<String> args = new ArrayList<>();
                int startIdx = op.equals("callr") ? 2 : 1; 
                for (int i = startIdx; i < operands.length; i++) {
                    args.add(operands[i].getValue());
                }

                // (`geti`) → syscall 5
                if (functionName.equals("geti")) {
                    ps.println("    li $v0, 5");
                    ps.println("    syscall");

                    if (dest1 != null) {
                        String reg = getRegister(dest1);
                        if (reg.equals("")) {
                            //ps.println("    move " + reg + ", $v0");
                            ps.println("    sw $v0, -" + getOff(dest1) + "($fp)");
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
                            ps.println("    sw $v0, -" + getOff(dest1) + "($fp)");
                        } else {
                            ps.println("    move " + reg + ", $v0");
                        }
                    }
                }

                //  (`puti`) → syscall 1
                else if (functionName.equals("puti")) {
                    if (isNumeric(arg)) {
                        ps.println("    li $a0, " + arg);
                    } else if (getRegister(arg).equals("")){
                        ps.println("    lw $a0, -" + getOff(arg) + "($fp)");
                    } else {
                        ps.println("    move $a0, " + getRegister(arg));
                    }
                    ps.println("    li $v0, 1");
                    ps.println("    syscall");
                }

                // (`putc`) → syscall 11
                else if (functionName.equals("putc")) {
                    if (isNumeric(arg)) {
                        ps.println("    li $a0, " + arg);
                    } else if (getRegister(arg).equals("")){
                        ps.println("    lw $a0, -" + getOff(arg) + "($fp)");
                    } else {
                        ps.println("    move $a0, " + getRegister(arg));
                    }
                    ps.println("    li $v0, 11");
                    ps.println("    syscall");
                } else {
                    // only need to worry about saving and restoring this block's basic registers
                    //if (func.instructions.get(func.instructions.indexOf(instruction) - 1).opCode != OpCode.LABEL) {
                        for (String virtReg : this.basicBlock.getVarUseSet().keySet()) {
                            String pReg = getRegister(virtReg);
                            if (!pReg.equals("")) {
                                ps.println("    sw " + pReg + ", -" + getOff(virtReg) + "($fp)");
                            }
                        }
                    //}
                    // arg handling 
                    // NOTE: Because we back up and restore registers around this we can reuse the same code from naive
                    int stackArgOffset = 0 ;
                    //System.out.println("ARGS SIZE + " + args.size() + ", OPERANDS SIZE = " + operands.length);
                    for (int i = 0; i < args.size(); i++) {
                        int offset = stackMap.getOrDefault(args.get(i), -1);
                        if (i < 4) {
                            if (offset == -1) {
                                ps.println("    li $a" + i + ", " + args.get(i));    
                            } else {
                                ps.println("    lw $a" + i + ", -" + offset + "($fp)");
                            }
                        } else {
                            if (offset == -1) {
                                int stackOffset = (i - 3) * 4;
                                ps.println("    li $t0, " + args.get(i));
                                ps.println("    sw $t0, -" + stackOffset + "($sp)");
                                stackArgOffset += 4;

                            } else {
                                int stackOffset = (i - 3) * 4;
                                ps.println("    lw $t0, -" + offset + "($fp)");  
                                ps.println("    sw $t0, -" + stackOffset + "($sp)");
                                stackArgOffset += 4;
                            }
                        }
                    }
                    if (stackArgOffset > 0) {
                        ps.println("    addi $sp, $sp, -" + stackArgOffset);
                    }
                    ps.println("    jal " + functionName);
                    if (dest1 != null) {
                        // String reg = getRegister(dest1);
                        //ps.println("    move $t0, $v0");
                        ps.println("    sw $v0, -" + stackMap.get(dest1) + "($fp)");
                    }
                    if (stackArgOffset > 0) {
                        ps.println("    addi $sp, $sp, " + stackArgOffset);
                    }
                    // restore return address
                    // ps.println("    lw $ra, 32($sp)");
                    for (String virtReg : this.basicBlock.getVarUseSet().keySet()) {
                        String pReg = getRegister(virtReg);
                        if (!pReg.equals("")) {
                            ps.println("    lw " + pReg + ", -" + getOff(virtReg) + "($fp)");
                        }
                    }
                }
                break;

            case "array_store":
                // array_store, arr, index, value → arr[index] = value
                String val = operands[0].getValue();
                int val_off = stackMap.get(val);
                String val_asm = "$t8";
                String arr = operands[1].getValue();
                int arr_addr_off = stackMap.get(arr);
                String arr_asm = getRegister(arr);
                String index = operands[2].getValue();

                // in order to avoid modifying the existing index value use $t9 for index/address modification
                if (isNumeric(index)) {
                    ps.println("    li $t9, " + index);
                } else if(getRegister(index).equals("")){
                    int idx_off = stackMap.get(index);
                    ps.println("    lw $t9, -" + idx_off + "($fp)");
                } else {
                    ps.println("    move $t9, " + getRegister(index));
                }

                if (getRegister(arr).equals("")) {
                    ps.println("    lw $t8, -" + arr_addr_off + "($fp)");
                    arr_asm = "$t8";
                }
                ps.println("    sll $t9, $t9, 2");
                ps.println("    add $t9, $t9, " + arr_asm);

                if ((!isNumeric(val)) && getRegister(val).equals("")) { 
                    ps.println("    lw $t8, -" + val_off + "($fp)");
                } else if (isNumeric(val)) {
                    ps.println("    li $t8, " + val);
                } else {
                    val_asm = getRegister(val);
                }
                
                ps.println("    sw " + val_asm + ", 0($t9)");
                break;

            case "array_load":
                // array_load, dest, arr, index → dest = arr[index]
                val = operands[0].getValue();
                val_off = stackMap.get(val);
                val_asm = "$t8";
                arr = operands[1].getValue();
                arr_addr_off = stackMap.get(arr);
                arr_asm = "$t8";
                index = operands[2].getValue();

                // in order to avoid modifying the existing index value use $t9 for index/address modification
                if (isNumeric(index)) {
                    ps.println("    li $t9, " + index);
                } else if(getRegister(index).equals("")){
                    int idx_off = stackMap.get(index);
                    ps.println("    lw $t9, -" + idx_off + "($fp)");
                } else {
                    ps.println("    move $t9, " + getRegister(index));
                }

                if (getRegister(arr).equals("")) {
                    ps.println("    lw $t8, -" + arr_addr_off + "($fp)");
                } else {
                    arr_asm = getRegister(arr);
                }
                ps.println("    sll $t9, $t9, 2");
                ps.println("    add $t9, $t9, " + arr_asm);
                // after this point t8 is unused and t9 holds the address of our array location

                boolean spill = getRegister(val).equals("");
                if (spill) { 
                    ps.println("    lw $t8, -" + val_off + "($fp)");
                } else {
                    val_asm = getRegister(val);
                }

                ps.println("    lw " + val_asm + ", 0($t9)");
                if (spill) {
                    ps.println("    sw $t8, - " + val_off + "($fp)");
                }
                break;

            case "label":
                String labelName = instruction.operands[0].getValue();
                String uniqueLabel =  c_function_name + "_" + labelName;
                ps.println(uniqueLabel + ":");
                break;

            default:
                System.out.println("Unsupported IR OpCode: " + op);
        }
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

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+");
    }


    public String getRegister(String operand) {
        int regNo = 8;
        if (operandToRegisterMap.containsKey(operand)) {
            regNo = operandToRegisterMap.get(operand);
        }
        if (regNo > 7) {
            return "";
        }

        String reg = "$t" + regNo;
        return reg;
    }

    public int getOff(String virtReg) {
        return this.stackMap.get(virtReg);
    }
}


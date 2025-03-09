import ir.IRFunction;
import ir.IRInstruction;
import ir.IRProgram;
import ir.datatype.IRArrayType;
import ir.datatype.IRIntType;
import ir.operand.IROperand;
import ir.operand.IRVariableOperand;

import java.io.PrintStream;
import java.util.*;

public class NaiveAllocator {

    private PrintStream ps;

    private int new_label;

    public NaiveAllocator(PrintStream ps) {
        this.ps = ps;
    }

    private Map<String, Integer> funcToArgCount = new HashMap<>();

    private int regIndex = 0;

    public void NaivePrintProgram(IRProgram program) {
        for (IRFunction function : program.functions) {
            funcToArgCount.put(function.name, function.parameters.size());
        }
        // ps.println(".data");


        // Map<String, String> variableMap = new LinkedHashMap<>();

        // for (IRFunction function : program.functions) {


        //     Set<String> paramNames = new HashSet<>();
        
        //     for (IRVariableOperand param : function.parameters)
        //         paramNames.add(param.getName());
        
        //     for (IRVariableOperand variable : function.variables) {
        //         if (paramNames.contains(variable.getName()))
        //             continue;
        
        //         String varName = variable.getName();
        
        //         if (variable.type instanceof IRArrayType) {
        //             IRArrayType arrayType = (IRArrayType) variable.type;
        //             int size = arrayType.getSize() * 4;
        //             String declaration = String.format("%s: .space %d", varName, size);
        //             variableMap.put(varName, declaration);
        //         } else {
        //             String declaration = (variable.type == IRIntType.get())
        //                 ? String.format("%s: .word 0", varName)
        //                 : String.format("%s: .float 0", varName);
        //             variableMap.put(varName, declaration);
        //         }
        //     }
        // }
        
        // for (String line : variableMap.values()) {
        //     ps.println(line);
        // }

        // IR-ASM mapping
        ps.println(" ");
        ps.println(".text");
        
        for (IRFunction function : program.functions) {
            if (function.name.equals("main")) {
                printFunction(function);
                ps.println("    li $v0, 10");
                ps.println("    syscall");
                ps.println();
                break; 
            }
        }

        for (IRFunction function : program.functions) {
            if (!function.name.equals("main")) {
                printFunction(function);
                ps.println("    jr $ra");
                ps.println();
            }
        }
    }

    // Hashmap stores virtual register name to byte offset from frame pointer
    public void printFunction(IRFunction function) {

        Set<String> paramNames = new HashSet<>();
        int local_size = 0;
        int arg_size = 0;
        HashMap<String, Integer> stackMap = new HashMap<String, Integer>();
        int fp_offset = 4;

        for (IRVariableOperand param : function.parameters) {
            paramNames.add(param.getName());
            stackMap.put(param.getName(), fp_offset);
            //System.out.println("param: " + param.getName() + " has offset: " + fp_offset);            
            fp_offset += 4;
            arg_size += 4;
        }
        
        for (IRVariableOperand variable : function.variables) {
            if (paramNames.contains(variable.getName()))
                continue;
        
            local_size += 4;
            stackMap.put(variable.getName(), fp_offset);
            //System.out.println("var: " + variable.getName() + " has offset: " + fp_offset);
            fp_offset += 4;
        }
        
        local_size += arg_size;


        ps.print(function.name);
        ps.println(':');
        
        ps.println("    addi $sp, $sp, -4");
        ps.println("    sw   $fp, 0($sp)");
        ps.println("    move $fp, $sp");

        int argOffset = (function.parameters.size() - 4) * 4;
        for (int i = 0; i < function.parameters.size(); i++) {
            int offset = stackMap.get(function.parameters.get(i).getName());
            if (i < 4) {
                ps.println("    sw $a" + i + ", -" + offset + "($fp)");
            } else {
                ps.println("    lw $t0, " + argOffset + "($fp)");  
                argOffset -= 4;

                ps.println("    sw $t0, -" + offset + "($fp)");
            }
        }

        ps.println("    addi $sp, $sp, -" + local_size);

       //MAYBE: align stack pointer to 32 bits 
        ps.println("    addi $sp, $sp, -4");

        ps.println("    sw   $ra, 0($sp)");
        
        // sbrk for array().
        // store address at local stack offset
        for (IRVariableOperand var : function.variables) {
            //System.out.println(var.getName());
            if (var.type instanceof IRArrayType && !function.parameters.contains(var)) {
                IRArrayType arr = (IRArrayType) var.type;
                ps.println("    li $v0, 9");
                ps.println("    li $a0, " + (arr.getSize()*4));
                ps.println("    syscall");
                ps.println("    sw $v0, -" + stackMap.get(var.getName()) + "($fp)");
            }
        }

        // Print instructions
        for (IRInstruction instruction : function.instructions) {
            printInstruction(function, instruction, stackMap);
        }

        if (function.name == "main") {
            ps.println("    li $v0, 10");
            ps.println("    syscall");
        }
        ps.println("    lw   $ra, 0($sp)");
        ps.println("    addi $sp, $sp, 4");
        ps.println("    addi $sp, $sp, " + local_size);

        ps.println("    lw   $fp, 0($sp)");
        ps.println("    addi $sp, $sp, 4");
    }

    public void printInstruction(IRFunction c_function, IRInstruction instruction, HashMap<String, Integer> stackMap) {

        String op = instruction.opCode.toString();
        IROperand[] operands = instruction.operands;

        switch (op) {
            case "assign":
                // assign, dest, value → li dest, value
                String dest = operands[0].getValue();
                int dest_off = stackMap.get(dest);
                String value = operands[1].getValue();
                if (isNumeric(value)) {
                    ps.println("    li $t1, " + value);
                } else {
                    int value_off = stackMap.get(value);
                    ps.println("    lw $t1, -" + value_off + "($fp)");
                }
                ps.println("    sw $t1, -" + dest_off + "($fp)");
                break;

            case "add":
            case "sub":
            case "mult":
            case "div":
            case "and":
            case "or":

                String res = operands[0].getValue();
                int res_off = stackMap.get(res);
                String op1 = operands[1].getValue();
                int op1_off = stackMap.get(op1);
                String op2 = operands[2].getValue();

                ps.println("    lw $t1, -" + op1_off + "($fp)");

                if (isNumeric(op2)) {
                    ps.println("    li $t2, " + op2 );
                } else {
                    int op2_off = stackMap.get(op2);
                    ps.println("    lw $t2, -" + op2_off + "($fp)");
                }
                if (op.equals("mult")) {
                    op = "mul";
                }

                ps.println("    " + op + " $t0, $t1, $t2");
                ps.println("    sw $t0, -" + res_off + "($fp)");
                break;

            case "goto":
                ps.println("    j " + c_function.name + "_"+ operands[0].getValue());
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

                if (isNumeric(cmp1)) {
                    ps.println("    li $t0, " + cmp1);
                } else {
                    int cmp1_off = stackMap.get(cmp1);
                    ps.println("    lw $t0, -" + cmp1_off + "($fp)");
                }

                if (isNumeric(cmp2)) {
                    ps.println("    li $t1, " + cmp2);
                } else {
                    int cmp2_off = stackMap.get(cmp2);
                    ps.println("    lw $t1, -" + cmp2_off + "($fp)");
                }

                String branchOp = getMIPSBranchOp(op);

                ps.println("    " + branchOp + " $t0, $t1, " + c_function.name + "_"+ lbl);
                break;

            case "return":
                // ps.println("    lw $ra, 32($sp)");
                ps.println("    jr $ra");
                break;

            case "call":
            case "callr":

                String dest1 = (op.equals("callr")) ? operands[0].getValue() : null;
                String functionName = (op.equals("callr")) ? operands[1].getValue() : operands[0].getValue();
                
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
                        ps.println("    move $t0, $v0");
                        ps.println("    sw $t0, -" + stackMap.get(dest1) + "($fp)");
                    }
                }

                // (`getc`) → syscall 12**
                else if (functionName.equals("getc")) {
                    ps.println("    li $v0, 12");
                    ps.println("    syscall");

                    if (dest1 != null) {
                        ps.println("    move $t0, $v0");
                        ps.println("    sw $t0, -" + stackMap.get(dest1) + "($fp)");
                    }
                }

                //  (`puti`) → syscall 1
                else if (functionName.equals("puti")) {
                    if (isNumeric(args.get(0))) {
                        ps.println("    li $a0, " + args.get(0));
                    } else {
                        ps.println("    lw $a0, -" + stackMap.get(args.get(0)) + "($fp)");
                    }
                    ps.println("    li $v0, 1");
                    ps.println("    syscall");
                }

                // (`putc`) → syscall 11
                else if (functionName.equals("putc")) {
                    if (isNumeric(args.get(0))) {
                        ps.println("    li $a0, " + args.get(0));
                    } else {
                        ps.println("    lw $a0, -" + stackMap.get(args.get(0))+ "($fp)");
                    }
                    ps.println("    li $v0, 11");
                    ps.println("    syscall");
                
                // regular fucntion call 
                } else {
                    // arg handling
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

                    ps.println("    addi $sp, $sp, -" + stackArgOffset);

                    ps.println("    jal " + functionName);
                    if (dest1 != null) {
                        // String reg = getRegister(dest1);
                        ps.println("    move $t0, $v0");
                        ps.println("    sw $t0, -" + stackMap.get(dest1) + "($fp)");
                    }

                    ps.println("    addi $sp, $sp, " + stackArgOffset);

                    // restore return address
                    // ps.println("    lw $ra, 32($sp)");

                }

                break;

            case "array_store":
                // array_store, value, arr, index, → arr[index] = value
                String val = operands[0].getValue();
                int val_off = stackMap.get(val);
                String arr = operands[1].getValue();
                int arr_addr_off = stackMap.get(arr);
                String index = operands[2].getValue();

                if (isNumeric(index)) {
                    ps.println("    li $t2, " + index);
                } else {
                    int idx_off = stackMap.get(index);
                    ps.println("    lw $t2, -" + idx_off + "($fp)");
                }

                if (isNumeric(val)) { 
                    ps.println("    lw $t0, " + val);
                } else {
                    ps.println("    lw $t0, -" + val_off + "($fp)");
                }

                ps.println("    lw $t1, -" + arr_addr_off + "($fp)");
                ps.println("    sll $t2, $t2, 2");
                ps.println("    add $t1, $t2, $t1");
                ps.println("    sw $t0, 0($t1)");
                break;

            case "array_load":
                // array_load, dest, arr, index → dest = arr[index]
                String loadDest = operands[0].getValue();
                int arr_dest_off = stackMap.get(loadDest);
                String loadArr = operands[1].getValue();
                int arr_addr_off_load = stackMap.get(loadArr);
                String loadIdx = operands[2].getValue();
                if (isNumeric(loadIdx)) {
                    ps.println("    li $t2, " + loadIdx);
                } else {
                    int loadIdx_off = stackMap.get(loadIdx);
                    ps.println("    lw $t2, -" + loadIdx_off + "($fp)");
                }
                ps.println("    sll $t2, $t2, 2");
                ps.println("    lw $t1, -" + arr_addr_off_load + "($fp)");
                ps.println("    add $t2, $t2, $t1");
                ps.println("    lw $t1, 0($t2)");
                ps.println("    sw $t1, -" + arr_dest_off +"($fp)");
                break;

            case "label":
                String labelName = instruction.operands[0].getValue();
                String uniqueLabel =  c_function.name + "_" + labelName;
                ps.println(uniqueLabel + ":");
                break;

            default:
                System.out.println("Unsupported IR OpCode: " + op);
        }
        // ps.println();
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


    public boolean isNumeric(String str) {
        return str.matches("-?\\d+");
    }


    public String getRegister(String operand) {
        Map<String, String> operandToRegisterMap = new HashMap<>();

        
        if (operandToRegisterMap.containsKey(operand)) {
            return operandToRegisterMap.get(operand);
        }
        
        
        if (regIndex >= 10) { 
            regIndex = 0;  
        }

        String reg = "$t" + regIndex;
        operandToRegisterMap.put(operand, reg);
        regIndex++;

        return reg;
    }
}


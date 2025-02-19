import ir.*;
import ir.datatype.IRArrayType;
import ir.datatype.IRIntType;
import ir.datatype.IRType;
import ir.operand.IRConstantOperand;
import ir.operand.IROperand;
import ir.operand.IRVariableOperand;

import java.io.PrintStream;
import java.util.*;

public class Demo {
    public static void main(String[] args) throws Exception {
        // Parse the IR file
        IRReader irReader = new IRReader();
        IRProgram program = irReader.parseIRFile(args[0]);

        // Print the IR to another file
        IRPrinter filePrinter = new IRPrinter(new PrintStream(args[1]));
        filePrinter.printProgram(program);

        // Create an IR printer that prints to stdout
        IRPrinter irPrinter = new IRPrinter(new PrintStream(System.out));

        // Print all instructions that stores a constant to an array
        System.out.println("Instructions that stores a constant to an array:");
        for (IRFunction function : program.functions) {
            for (IRInstruction instruction : function.instructions) {
                if (instruction.opCode == IRInstruction.OpCode.ARRAY_STORE) {
                    if (instruction.operands[0] instanceof IRConstantOperand) {
                        System.out.print(String.format("Line %d:", instruction.irLineNumber));
                        irPrinter.printInstruction(instruction);
                    }
                }
            }
        }
        System.out.println();

        // Print the name of all int scalars and int arrays with a size of 1
        System.out.println("Int scalars and 1-sized arrays:");
        for (IRFunction function : program.functions) {
            List<String> vars = new ArrayList<>();
            for (IRVariableOperand v : function.variables) {
                IRType type = v.type;
                // For each unique data type, only one IRType object will be created
                // so that IRType objects can be compared using '=='
                if (type == IRIntType.get() || type == IRArrayType.get(IRIntType.get(), 1))
                    vars.add(v.getName());
            }
            if (!vars.isEmpty())
                System.out.println(function.name + ": " + String.join(", ", vars));
        }
        System.out.println();

        // Print all variables that are declared but not used (including unused parameters)
        System.out.println("Unused variables/parameters:");
        for (IRFunction function : program.functions) {
            // IROperand objects are not shared between instructions/parameter list/variable list
            // They should be compared using their names
            Set<String> vars = new HashSet<>();
            // Parameters are not included in the variable list
            for (IRVariableOperand v : function.parameters)
                vars.add(v.getName());
            for (IRVariableOperand v : function.variables)
                vars.add(v.getName());
            for (IRInstruction instruction : function.instructions)
                for (IROperand operand : instruction.operands)
                    if (operand instanceof IRVariableOperand) {
                        IRVariableOperand variableOperand = (IRVariableOperand) operand;
                        vars.remove(variableOperand.getName());
                    }
            if (!vars.isEmpty())
                System.out.println(function.name + ": " + String.join(", ", vars));
        }
        System.out.println();
    }
}

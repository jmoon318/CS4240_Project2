package ir;

import ir.datatype.IRArrayType;
import ir.datatype.IRIntType;
import ir.operand.IROperand;
import ir.operand.IRVariableOperand;

import java.io.PrintStream;
import java.util.*;

public class IRPrinter {

    private PrintStream ps;

    public IRPrinter(PrintStream ps) {
        this.ps = ps;
    }

    public void printProgram(IRProgram program) {
        for (IRFunction function : program.functions) {
            printFunction(function);
            ps.println();
        }
    }

    public void printFunction(IRFunction function) {
        ps.println("#start_function");

        // Print signature
        if (function.returnType == null)
            ps.print("void");
        else
            ps.print(function.returnType);
        ps.print(' ');
        ps.print(function.name);
        ps.print('(');
        boolean first = true;
        for (IRVariableOperand param : function.parameters) {
            if (first)
                first = false;
            else
                ps.print(", ");
            ps.print(param.type);
            ps.print(' ');
            ps.print(param.getName());
        }
        ps.println("):");

        // Print variable lists
        Set<String> paramNames = new HashSet<>();
        for (IRVariableOperand param : function.parameters)
            paramNames.add(param.getName());
        List<String> intList = new ArrayList<>();
        List<String> floatList = new ArrayList<>();
        for (IRVariableOperand variable : function.variables) {
            if (paramNames.contains(variable.getName()))
                continue;
            if (variable.type instanceof IRArrayType) {
                IRArrayType arrayType = (IRArrayType)variable.type;
                if (arrayType.getElementType() == IRIntType.get())
                    intList.add(String.format("%s[%d]", variable.getName(), arrayType.getSize()));
                else
                    floatList.add(String.format("%s[%d]", variable.getName(), arrayType.getSize()));
            } else {
                if (variable.type == IRIntType.get())
                    intList.add(variable.getName());
                else
                    floatList.add(variable.getName());
            }
        }
        ps.print("int-list: ");
        ps.println(String.join(", ", intList));
        ps.print("float-list: ");
        ps.println(String.join(", ", floatList));

        // Print instructions
        for (IRInstruction instruction : function.instructions) {
            if (instruction.opCode != IRInstruction.OpCode.LABEL)
                ps.print("    ");
            printInstruction(instruction);
        }

        ps.println("#end_function");
    }

    public void printInstruction(IRInstruction instruction) {
        if (instruction.opCode == IRInstruction.OpCode.LABEL) {
            ps.print(instruction.operands[0]);
            ps.print(":");
            ps.println();
            return;
        }
        ps.print(instruction.opCode);
        for (IROperand operand : instruction.operands) {
            ps.print(", ");
            ps.print(operand);
        }
        ps.println();
    }

}

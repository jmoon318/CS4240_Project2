package ir;

import ir.datatype.IRType;
import ir.operand.IRVariableOperand;

import java.util.ArrayList;
import java.util.List;

public class IRFunction {

    public String name;

    public IRType returnType;

    public List<IRVariableOperand> parameters;

    public List<IRVariableOperand> variables;

    public List<IRInstruction> instructions;

    public List<IRInstruction> worklist = new ArrayList<IRInstruction>();

    public IRFunction(String name, IRType returnType,
                      List<IRVariableOperand> parameters, List<IRVariableOperand> variables,
                      List<IRInstruction> instructions) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.variables = variables;
        this.instructions = instructions;
    }

    public IRInstruction getInstruction(int lineNo) {
        int start = this.instructions.get(0).irLineNumber;
        return this.instructions.get(lineNo - start);
    }
}

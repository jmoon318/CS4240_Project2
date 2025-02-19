package ir.operand;

import ir.IRInstruction;

public class IRFunctionOperand extends IROperand {

    public IRFunctionOperand(String name, IRInstruction parent) {
        super(name, parent);
    }

    public String getName() {
        return value;
    }
}

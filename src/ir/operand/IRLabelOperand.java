package ir.operand;

import ir.IRInstruction;

public class IRLabelOperand extends IROperand {

    public IRLabelOperand(String name, IRInstruction parent) {
        super(name, parent);
    }

    public String getName() {
        return value;
    }
}

package ir.operand;

import ir.IRInstruction;

public abstract class IROperand {

    protected String value;

    protected IRInstruction parent;

    public IROperand(String value, IRInstruction parent) {
        this.value = value;
        this.parent = parent;
    }

    public IRInstruction getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof IROperand) {
            IROperand otherOp = (IROperand) other;
            return this.value.equals(otherOp.value);
        }
        return false;
    }

}

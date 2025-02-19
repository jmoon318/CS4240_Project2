package ir.operand;

import ir.IRInstruction;
import ir.datatype.IRType;

public class IRConstantOperand extends IROperand {

    public IRType type;

    public IRConstantOperand(IRType type, String value, IRInstruction parent) {
        super(value, parent);
        this.type = type;
    }

    public String getValueString() {
        return value;
    }
}

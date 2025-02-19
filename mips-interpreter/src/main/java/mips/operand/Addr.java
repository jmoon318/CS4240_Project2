package main.java.mips.operand;

public class Addr extends MIPSOperand {

    public enum Mode {
        PC_RELATIVE, REGISTER, BASE_OFFSET
    }

    public Mode mode;

    public String label;
    public Register register;
    public Imm constant;

    public Addr(String label) {
        this.label = label;
        this.mode = Mode.PC_RELATIVE;
    }

    public Addr(Register reg) {
        register = reg;
        this.mode = Mode.REGISTER;
    }

    public Addr(Imm constant, Register reg) {
        this.constant = constant;
        register = reg;
        this.mode = Mode.BASE_OFFSET;
    }

    @Override
    public String toString() {
        switch (mode) {
            case PC_RELATIVE:
                return label;
            case REGISTER:
                return "(" + register + ")";
            case BASE_OFFSET:
                return constant.toString() + "(" + register + ")";
            default:
                return null;
        }
    }
}

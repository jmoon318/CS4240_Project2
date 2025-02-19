package main.java.mips;

public enum MIPSOp {
    ADD, ADDI, SUB, MUL, DIV, AND, ANDI, OR, ORI, SLL, // bin ops
    LI, LW, MOVE, SW, LA, // data movement
    BEQ, BNE, BLT, BGT, BGE, // branches
    J, JAL, JR, // jumps
    SYSCALL,
    // single-precision fp arithmetic
    ADD_S(true), ADDI_S(true), SUB_S(true), MUL_S(true), DIV_S(true),
    // single-precision fp data movement
    LI_S(true), MOV_S(true), L_S(true), S_S(true),
    // single-precision branching
    C_EQ_S(true), C_NE_S(true), C_LT_S(true), C_GT_S(true), C_GE_S(true),
    // double-precision fp arithmetic
    ADD_D(true), ADDI_D(true), SUB_D(true), MUL_D(true), DIV_D(true),
    // double-precision fp data movement
    LI_D(true), MOV_D(true), L_D(true), S_D(true),
    // double-precision branching
    C_EQ_D(true), C_NE_D(true), C_LT_D(true), C_GT_D(true), C_GE_D(true),
    BC1T, BC1F;

    public boolean floatOp;
    public String precision;

    MIPSOp() {
        this(false);
    }

    MIPSOp(boolean floatOp) {
        this.floatOp = floatOp;
        if (floatOp) {
            String name = name().toLowerCase();
            precision = name.substring(name.length() - 1, name.length());
        }
    }

    @Override
    public String toString() {
        if (floatOp) {
            String name = name().toLowerCase();
            return name.replaceAll("_", ".");
        }

        return name().toLowerCase();
    }
}
package main.java.mips;

import java.util.Arrays;
import java.util.List;

import main.java.mips.operand.MIPSOperand;
import main.java.mips.operand.Register;
import main.java.mips.operand.Addr;

public class MIPSInstruction {

    public static final int WORD_SIZE = 4;

    // binary operation operand order
    public static final int R_D_BOP = 0;
    public static final int R_S_BOP = 1;
    public static final int R_T_BOP = 2;

    // operand order for LI
    public static final int R_D_LI = 0;
    public static final int IMM_LI = 1;

    // operand order for LW
    public static final int R_D_LW = 0;
    public static final int ADDR_LW = 1;

    // operand order for MOVE
    public static final int R_D_MOV = 0;
    public static final int R_S_MOV = 1;

    // operand order for SW
    public static final int R_S_SW = 0;
    public static final int ADDR_SW = 1;

    // operand order for branches
    public static final int R_S_BR = 0;
    public static final int R_T_BR = 1;
    public static final int LABEL_BR = 2;

    //operand order for J
    public static final int LABEL_J = 0;

    // operand order for JR
    public static final int R_S_JR = 0;


    public MIPSOp op;
    public String label;
    public List<MIPSOperand> operands;


    public MIPSInstruction(MIPSOp op, String label, MIPSOperand... operands) {
        this.op = op;
        this.label = label;
        this.operands = Arrays.asList(operands);
    }

    public boolean isBranch() {
        switch (op) {
            case BEQ:
            case BNE:
            case BLT:
            case BGT:
            case BGE:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (label != null) {
            builder.append(label + ": ");
        }

        builder.append(op.toString());
        if (operands.isEmpty()) {
            return builder.toString();
        }
        builder.append(' ');

        for (int i = 0; i < operands.size() - 1; i++) {
            MIPSOperand operand = operands.get(i);
            builder.append(operand.toString());
            builder.append(", ");
        }

        builder.append(operands.get(operands.size() - 1));

        return builder.toString();
    }

    public Register[] getReads() {
        Register[] reads;

        switch (op) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case AND:
            case OR:
            case ADD_S:
            case SUB_S:
            case MUL_S:
            case DIV_S:
            case ADD_D:
            case SUB_D:
            case MUL_D:
            case DIV_D:
                reads = new Register[2];
                reads[0] = (Register)operands.get(R_S_BOP);
                reads[1] = (Register)operands.get(R_T_BOP);
                return reads;
            case ADDI:
            case ANDI:
            case ORI:
            case SLL:
            case ADDI_S:
            case ADDI_D:
                reads = new Register[1];
                reads[0] = (Register)operands.get(R_S_BOP);
                return reads;
            case LW:
            case L_S:
            case L_D:
                reads = new Register[1];
                reads[0] = ((Addr)operands.get(ADDR_LW)).register;
                return reads;
            case MOVE:
            case MOV_S:
            case MOV_D:
                reads = new Register[1];
                reads[0] = (Register)operands.get(R_S_MOV);
                return reads;
            case SW:
            case S_S:
            case S_D:
                reads = new Register[2];
                reads[0] = (Register)operands.get(R_S_SW);
                reads[1] = ((Addr)operands.get(ADDR_SW)).register;
                return reads;
            case BEQ:
            case BNE:
            case BLT:
            case BGT:
            case BGE:
            case C_EQ_S:
            case C_NE_S:
            case C_LT_S:
            case C_GT_S:
            case C_GE_S:
            case C_EQ_D:
            case C_NE_D:
            case C_LT_D:
            case C_GT_D:
            case C_GE_D:
                reads = new Register[2];
                reads[0] = (Register)operands.get(R_S_BR);
                reads[1] = (Register)operands.get(R_T_BR);
                return reads;
            case JR:
                reads = new Register[1];
                reads[0] = (Register)operands.get(R_S_JR);
                return reads;
            default:
                return new Register[0];
        }
    }

    public Register getWrite() {
        switch (op) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case AND:
            case OR:
            case ADDI:
            case ANDI:
            case ORI:
            case SLL:
            case ADD_S:
            case SUB_S:
            case MUL_S:
            case DIV_S:
            case ADD_D:
            case SUB_D:
            case MUL_D:
            case DIV_D:
                return (Register)operands.get(R_D_BOP);
            case LI:
            case LA:
            case LI_S:
            case LI_D:
                return (Register)operands.get(R_D_LI);
            case LW:
            case L_S:
            case L_D:
                return (Register)operands.get(R_D_LW);
            case MOVE:
            case MOV_S:
            case MOV_D:
                return (Register)operands.get(R_D_MOV);
            default:
                return null;
        }
    }
}

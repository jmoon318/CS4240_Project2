package ir;

import ir.operand.IROperand;
import ir.operand.IRVariableOperand;

import java.util.ArrayList;

public class IRInstruction {

    public enum OpCode {
        ASSIGN,
        ADD, SUB, MULT, DIV, AND, OR,
        GOTO,
        BREQ, BRNEQ, BRLT, BRGT, BRGEQ,
        RETURN,
        CALL, CALLR,
        ARRAY_STORE, ARRAY_LOAD,
        LABEL;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public OpCode opCode;

    public IROperand[] operands;

    public int irLineNumber;

    public String defID;

    public IRInstruction() {}

    public boolean gen;

    public int assignedBlock;

    public boolean critical;

    public boolean mark;

    private ArrayList<String> useSet = new ArrayList<>();


    public IRInstruction(OpCode opCode, IROperand[] operands, int irLineNumber) {
        this.opCode = opCode;
        this.operands = operands;
        this.irLineNumber = irLineNumber;
        this.defID = null;
        this.assignedBlock = -1;
        this.critical = false;
        this.mark = false;

    }

    public void setDefID(String defID) {
        this.defID = defID;
    }

    public String getDefID() {
        return this.defID;
    }

    public void setGen(boolean gen) {
        this.gen = gen;
    }

    public void setAssignedBlock(int blockID) {
        this.assignedBlock = blockID;
    }

    public int getAssignedBlock() {
        return assignedBlock;
    }

    public boolean getCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public void setMark(boolean mark) {
        this.mark = mark;
    }


    public void addUse(String variable) {
        if (!useSet.contains(variable)) {
            this.useSet.add(variable);
        }
    }

    public ArrayList<String> getUseSet() {
        return useSet;
    }

    public IROperand getDefOperand() {
        if (isDefinitionOp(this.opCode)) {
            if (this.opCode == OpCode.ARRAY_STORE) {
                return this.operands[1];
            } else {
                return this.operands[0];
            }
        }
        return this.operands[0];
    }   

    @Override
    public String toString() {
        String out = this.opCode.toString();
        for (IROperand operand : this.operands) {
            out += ", " + operand.toString();
        }
        return out;
    }
   private static boolean isDefinitionOp(OpCode opCode) {
        return opCode == OpCode.ADD || opCode == OpCode.SUB ||
                opCode == OpCode.MULT || opCode == OpCode.DIV ||
                opCode == OpCode.AND || opCode == OpCode.OR ||
                opCode == OpCode.ASSIGN || opCode == OpCode.CALLR ||
                opCode == OpCode.ARRAY_LOAD;
    }
}

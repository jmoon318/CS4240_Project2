package main.java.mips.operand;

public class Imm extends MIPSOperand {

    private String val;
    private String type;

    public Imm(String val, String type) {
        this.val = val;
        this.type = type;
    }

    public int getInt() {
        switch (type) {
            case "HEX":
                return Integer.decode(val);
            case "DEC":
                return Integer.parseInt(val);
            default:
                return 0;
        }
    }

    public float getSingle() {
        if (type.equals("SINGLE")) {
            return Float.parseFloat(val);
        }

        throw new IllegalArgumentException();
    }

    public double getDouble() {
        if (type.equals("DOUBLE")) {
            return Double.parseDouble(val);
        }

        throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
        return val;
    }
}

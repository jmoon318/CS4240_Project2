package ir.datatype;

public class IRIntType extends IRType {

    private static IRIntType instance;

    private IRIntType() {}

    public static IRIntType get() {
        if (instance == null) {
            instance = new IRIntType();
        }
        return instance;
    }

    @Override
    public String toString() {
        return "int";
    }

}

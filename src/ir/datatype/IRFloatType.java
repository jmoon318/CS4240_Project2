package ir.datatype;

public class IRFloatType extends IRType {

    private static IRFloatType instance;

    private IRFloatType() {}

    public static IRFloatType get() {
        if (instance == null) {
            instance = new IRFloatType();
        }
        return instance;
    }

    @Override
    public String toString() {
        return "float";
    }

}

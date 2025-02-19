package ir.datatype;

import java.util.Map;
import java.util.HashMap;

public class IRArrayType extends IRType {

    private static Map<IRType, Map<Integer, IRArrayType>> instances = new HashMap<>();

    private IRType elementType;

    private int size;

    private IRArrayType(IRType elementType, int size) {
        this.elementType = elementType;
        this.size = size;
    }

    public static IRArrayType get(IRType elementType, int size) {
        return instances
                .computeIfAbsent(elementType, k -> new HashMap<>())
                .computeIfAbsent(size, k -> new IRArrayType(elementType, size));
    }

    public IRType getElementType() {
        return elementType;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return String.format("%s[%d]", elementType.toString(), size);
    }

}

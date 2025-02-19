package ir;

import java.util.ArrayList;
import java.util.List;

public class IRProgram {

    public List<IRFunction> functions;

    public IRProgram() {
        functions = new ArrayList<>();
    }

    public IRProgram(List<IRFunction> functions) {
        this.functions = functions;
    }

}

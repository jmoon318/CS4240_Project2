package ir;

public class IRException extends Exception {

    public IRException() {}

    public IRException(String s) { super(s); }

    public IRException(String s, int lineNumber) { super(s + " at line " + lineNumber); }

}

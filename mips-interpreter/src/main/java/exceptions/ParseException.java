package main.java.exceptions;

public class ParseException extends RuntimeException {

    public ParseException() {
        super();
    }

    public ParseException(String msg) {
        super(msg);
    }
}

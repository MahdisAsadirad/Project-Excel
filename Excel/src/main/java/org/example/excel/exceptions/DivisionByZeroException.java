package org.example.excel.exceptions;

public class DivisionByZeroException extends SpreadsheetException {
    public DivisionByZeroException() {
        super("Division by zero");
    }

    public DivisionByZeroException(String operation) {
        super("Division by zero in operation: " + operation);
    }
}

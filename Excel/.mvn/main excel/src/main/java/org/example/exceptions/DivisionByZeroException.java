package org.example.exceptions;

public class DivisionByZeroException extends SpreadsheetException {
    public DivisionByZeroException(String operation) {
        super("Division by zero in operation: " + operation);
    }
}


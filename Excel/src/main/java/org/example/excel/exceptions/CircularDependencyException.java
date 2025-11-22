package org.example.excel.exceptions;

public class CircularDependencyException extends SpreadsheetException {
    public CircularDependencyException(String message) {
        super(message);
    }

    public CircularDependencyException(String cell1, String cell2) {
        super("Circular dependency detected between " + cell1 + " and " + cell2);
    }
}
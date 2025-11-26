package org.example.exceptions;

public class CircularDependencyException extends SpreadsheetException {
    public CircularDependencyException(String message) {
        super(message);
    }
}
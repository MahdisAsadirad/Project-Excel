package org.example.exceptions;

public abstract class SpreadsheetException extends RuntimeException {
    public SpreadsheetException(String message) {
        super(message);
    }
}
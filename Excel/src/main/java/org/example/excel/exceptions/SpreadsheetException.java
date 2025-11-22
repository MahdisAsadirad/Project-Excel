package org.example.excel.exceptions;

public abstract class SpreadsheetException extends RuntimeException {
    public SpreadsheetException(String message) {
        super(message);
    }

    public SpreadsheetException(String message, Throwable cause) {
        super(message, cause);
    }
}
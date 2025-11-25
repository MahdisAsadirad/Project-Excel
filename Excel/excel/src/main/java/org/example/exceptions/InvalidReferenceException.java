package org.example.exceptions;

public class InvalidReferenceException extends SpreadsheetException {
    public InvalidReferenceException(String cellReference) {
        super("Invalid cell reference: " + cellReference);
    }
}

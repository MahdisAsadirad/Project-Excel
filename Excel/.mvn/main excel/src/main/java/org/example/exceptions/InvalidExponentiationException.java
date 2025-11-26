package org.example.exceptions;

public class InvalidExponentiationException extends SpreadsheetException {
    public InvalidExponentiationException(String message) {
        super(message);
    }

    public InvalidExponentiationException(String base, String exponent) {
        super("Invalid exponentiation: base=" + base + ", exponent=" + exponent);
    }
}

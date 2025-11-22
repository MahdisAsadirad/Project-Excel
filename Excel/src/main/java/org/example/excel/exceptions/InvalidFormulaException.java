package org.example.excel.exceptions;

public class InvalidFormulaException extends SpreadsheetException {
    public InvalidFormulaException(String message) {
        super(message);
    }

    public InvalidFormulaException(String formula, String reason) {
        super("Invalid formula '" + formula + "': " + reason);
    }
}

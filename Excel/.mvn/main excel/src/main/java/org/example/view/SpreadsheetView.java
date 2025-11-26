package org.example.view;

import org.example.model.Cell;
import org.example.model.ErrorType;
import org.example.model.Spreadsheet;
import org.example.utils.CellConverter;

import java.util.List;
import java.util.Map;

public class SpreadsheetView {
    private final Spreadsheet spreadsheet;

    public SpreadsheetView(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
    }

    public void displaySpreadsheet() {
        displayHeader();
        displayRows();
        displayErrors();
    }


    private void displayHeader() {
        System.out.print("   ");
        for (int col = 0; col < spreadsheet.getCols(); col++) {
            System.out.printf("%-12s", CellConverter.getColumnName(col));
        }
        System.out.println();
    }

    private void displayRows() {
        for (int row = 0; row < spreadsheet.getRows(); row++) {
            System.out.printf("%-3d", row + 1);

            for (int col = 0; col < spreadsheet.getCols(); col++) {
                Cell cell = spreadsheet.getCell(row, col);
                String displayValue = formatCellForDisplay(cell);
                System.out.printf("%-12s", displayValue);
            }
            System.out.println();
        }
    }


    private String formatCellForDisplay(Cell cell) {

        if (cell.hasError())
            return "#ERR!";

        switch (cell.getCellType()) {

            case EMPTY:
                return "-";

            case TEXT:
                return truncateText(cell.getStringValue(), 8);

            case NUMBER:
            case FORMULA:
                Object value = cell.getComputedValue();
                if (value == null)
                    return "-";
                if (value instanceof Integer)
                    return value.toString();
                if (value instanceof Double)
                    return formatNumber((Double) value);
                return truncateText(value.toString(), 8);

            case ERROR:
                return "#ERR!";
        }

        return "-";
    }


    private String formatNumber(double value) {

        if (Double.isInfinite(value) || Double.isNaN(value))
            return "#ERR!";

        if (value == Math.floor(value))
            return String.valueOf((long) value);

        String formatted = String.format("%.4f", value);
        if (formatted.length() > 10) {
            return String.format("%.3E", value);
        }
        return formatted;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return
                "-";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }


    public void displayErrors() {
        Map<ErrorType, List<String>> errorReport = spreadsheet.getErrorReport();

        boolean hasErrors = false;
        for (ErrorType errorType : ErrorType.values()) {
            if (errorType == ErrorType.NO_ERROR) continue;

            List<String> cells = errorReport.get(errorType);
            if (cells == null || cells.isEmpty()) continue;

            if (!hasErrors) {
                System.out.println("\n!!! ERRORS FOUND !!!");
                hasErrors = true;
            }

            displayErrorType(errorType, cells);
        }
    }

    private void displayErrorType(ErrorType errorType, List<String> cellRefs) {
        System.out.println("\n" + errorType.getDescription() + ":");
        for (String ref : cellRefs) {
            Cell cell = spreadsheet.getCell(ref);
            String msg;

            if (cell != null && cell.getErrorMessage() != null) {
                msg = cell.getErrorMessage();
            } else {
                msg = "(no message)";
            }

            System.out.printf("  %s : %s%n", ref, msg);
        }
    }



    public void displayCellDetails(String cellReference) {

        if (!spreadsheet.isValidCellReference(cellReference)) {
            System.out.println("Invalid Reference: " + cellReference);
            return;
        }

        Cell cell = spreadsheet.getCell(cellReference);
        if (cell == null) {
            System.out.println("Cell is empty.");
            return;
        }

        System.out.println("\n((CELL DETAILS))");
        System.out.println("Cell: " + cellReference);
        System.out.println("Type: " + cell.getCellType());
        System.out.println("Raw: " + cell.getRawContent());
        System.out.println("Computed: " + cell.getComputedValue());
        System.out.println("Error: " + (cell.hasError() ? cell.getErrorMessage() : "None"));
        System.out.println("====================\n");
    }

    public String displayGridStatistics() {

        int totalCells = spreadsheet.getRows() * spreadsheet.getCols();
        int empty = 0, number = 0, text = 0, formula = 0, error = 0;

        for (int r = 0; r < spreadsheet.getRows(); r++) {
            for (int c = 0; c < spreadsheet.getCols(); c++) {
                switch (spreadsheet.getCell(r, c).getCellType()) {
                    case EMPTY:
                        empty++;
                        break;
                    case NUMBER:
                        number++;
                        break;
                    case TEXT:
                        text++;
                        break;
                    case FORMULA:
                        formula++;
                        break;
                    case ERROR:
                        error++;
                        break;
                }
            }
        }

        return "\n((( Grid Statistics )))\n" +
                "Total Cells : " + totalCells + "\n" +
                "Empty       : " + empty + " (" + percent(empty, totalCells) + "%)\n" +
                "Numbers     : " + number + " (" + percent(number, totalCells) + "%)\n" +
                "Text        : " + text + " (" + percent(text, totalCells) + "%)\n" +
                "Formulas    : " + formula + " (" + percent(formula, totalCells) + "%)\n" +
                "Errors      : " + error + " (" + percent(error, totalCells) + "%)";
    }

    private String percent(int value, int total) {
        return String.format("%.2f", (value * 100.0) / total);
    }
}

package org.example.excel.view;

import org.example.excel.model.Cell;
import org.example.excel.model.ErrorType;
import org.example.excel.model.Spreadsheet;
import org.example.excel.utils.CellReferenceConverter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpreadsheetView {
    private final Spreadsheet spreadsheet;
    private final ErrorReporter errorReporter;

    public SpreadsheetView(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
        this.errorReporter = new ErrorReporter(spreadsheet);
    }

    public void displaySpreadsheet() {
        displayHeader();
        displayRows();
        displayErrors();
    }

    private void displayHeader() {
        // نمایش هدر ستون‌ها
        System.out.print("     ");
        for (int col = 0; col < spreadsheet.getCols(); col++) {
            System.out.print(String.format("%-10s", CellReferenceConverter.getColumnName(col)));
        }
        System.out.println();

        // نمایش خط جداکننده
        System.out.print("     ");
        for (int col = 0; col < spreadsheet.getCols(); col++) {
            System.out.print("----------");
            if (col < spreadsheet.getCols() - 1) {
                System.out.print("-");
            }
        }
        System.out.println();
    }

    private void displayRows() {
        for (int row = 0; row < spreadsheet.getRows(); row++) {
            // نمایش شماره سطر
            System.out.print(String.format("%-3d | ", row + 1));

            // نمایش مقادیر سلول‌ها
            for (int col = 0; col < spreadsheet.getCols(); col++) {
                Cell cell = spreadsheet.getCell(row, col);
                String displayValue = formatCellForDisplay(cell);
                System.out.print(String.format("%-10s", displayValue));

                if (col < spreadsheet.getCols() - 1) {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }

    private String formatCellForDisplay(Cell cell) {
        if (cell.hasError()) {
            return "#ERR!";
        }

        switch (cell.getCellType()) {
            case EMPTY:
                return "--";
            case TEXT:
                String textValue = cell.getStringValue();
                return truncateText(textValue, 8);
            case NUMBER:
            case FORMULA:
                Object value = cell.getComputedValue();
                if (value instanceof Double) {
                    return formatNumber((Double) value);
                } else if (value instanceof Integer) {
                    return String.valueOf(value);
                } else if (value != null) {
                    return truncateText(value.toString(), 8);
                } else {
                    return "--";
                }
            case ERROR:
                return "#ERR!";
            default:
                return "--";
        }
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        } else {
            // محدود کردن به ۲ رقم اعشار و ۸ کاراکتر کل
            String formatted = String.format("%.2f", value);
            return formatted.length() > 8 ? formatted.substring(0, 8) : formatted;
        }
    }

    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        } else {
            return text.substring(0, maxLength - 3) + "...";
        }
    }

    public void displayErrors() {
        Map<ErrorType, List<String>> errorReport = errorReporter.getErrorReport();
        boolean hasErrors = false;

        for (ErrorType errorType : ErrorType.values()) {
            List<String> errorCells = errorReport.get(errorType);
            if (errorType != ErrorType.NO_ERROR && !errorCells.isEmpty()) {
                if (!hasErrors) {
                    System.out.println("\n=== ERROR REPORT ===");
                    hasErrors = true;
                }
                displayErrorType(errorType, errorCells);
            }
        }

        if (!hasErrors) {
            System.out.println("\nNo errors in spreadsheet.");
        }
    }

    private void displayErrorType(ErrorType errorType, List<String> errorCells) {
        System.out.println("\n" + errorType.getDescription() + ":");
        for (String cellRef : errorCells) {
            Cell cell = spreadsheet.getCell(cellRef);
            System.out.printf("  %s: %s%n", cellRef, cell.getErrorMessage());
        }
    }

    public void displayCellDetails(String cellReference) {
        if (!spreadsheet.isValidCellReference(cellReference)) {
            System.out.println("Invalid cell reference: " + cellReference);
            return;
        }

        Cell cell = spreadsheet.getCell(cellReference);
        System.out.println("\n=== Cell Details: " + cellReference + " ===");
        System.out.println("Raw Content: " + cell.getRawContent());
        System.out.println("Cell Type: " + cell.getCellType());
        System.out.println("Computed Value: " + cell.getComputedValue());
        System.out.println("Has Error: " + cell.hasError());

        if (cell.hasError()) {
            System.out.println("Error Type: " + cell.getErrorType());
            System.out.println("Error Message: " + cell.getErrorMessage());
        }

        Set<String> dependencies = cell.getDependencies();
        if (!dependencies.isEmpty()) {
            System.out.println("Dependencies: " + String.join(", ", dependencies));
        }

        Set<String> dependents = spreadsheet.getDependents(cellReference);
        if (!dependents.isEmpty()) {
            System.out.println("Dependents: " + String.join(", ", dependents));
        }
    }

    public void displayGridStatistics() {
        int totalCells = spreadsheet.getRows() * spreadsheet.getCols();
        int emptyCells = 0;
        int numberCells = 0;
        int textCells = 0;
        int formulaCells = 0;
        int errorCells = 0;

        for (int row = 0; row < spreadsheet.getRows(); row++) {
            for (int col = 0; col < spreadsheet.getCols(); col++) {
                Cell cell = spreadsheet.getCell(row, col);
                switch (cell.getCellType()) {
                    case EMPTY:
                        emptyCells++;
                        break;
                    case NUMBER:
                        numberCells++;
                        break;
                    case TEXT:
                        textCells++;
                        break;
                    case FORMULA:
                        formulaCells++;
                        break;
                    case ERROR:
                        errorCells++;
                        break;
                }
            }
        }

        System.out.println("\n=== Grid Statistics ===");
        System.out.println("Total Cells: " + totalCells);
        System.out.println("Empty Cells: " + emptyCells + " (" + (emptyCells * 100 / totalCells) + "%)");
        System.out.println("Number Cells: " + numberCells + " (" + (numberCells * 100 / totalCells) + "%)");
        System.out.println("Text Cells: " + textCells + " (" + (textCells * 100 / totalCells) + "%)");
        System.out.println("Formula Cells: " + formulaCells + " (" + (formulaCells * 100 / totalCells) + "%)");
        System.out.println("Error Cells: " + errorCells + " (" + (errorCells * 100 / totalCells) + "%)");
    }
}
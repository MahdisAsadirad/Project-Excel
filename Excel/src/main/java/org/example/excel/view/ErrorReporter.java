package org.example.excel.view;


import org.example.excel.model.Cell;
import org.example.excel.model.ErrorType;
import org.example.excel.model.Spreadsheet;
import org.example.excel.utils.CellReferenceConverter;

import java.util.*;

public class ErrorReporter {
    private final Spreadsheet spreadsheet;

    public ErrorReporter(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
    }

    public Map<ErrorType, List<String>> getErrorReport() {
        Map<ErrorType, List<String>> errorReport = new EnumMap<>(ErrorType.class);

        for (ErrorType errorType : ErrorType.values()) {
            errorReport.put(errorType, new ArrayList<>());
        }

        // جمع‌آوری سلول‌های خطادار
        for (int row = 0; row < spreadsheet.getRows(); row++) {
            for (int col = 0; col < spreadsheet.getCols(); col++) {
                Cell cell = spreadsheet.getCell(row, col);
                if (cell.hasError()) {
                    String cellRef = CellReferenceConverter.toCellReference(row, col);
                    errorReport.get(cell.getErrorType()).add(cellRef);
                }
            }
        }

        return errorReport;
    }

    public List<String> getAllErrorCells() {
        List<String> errorCells = new ArrayList<>();

        for (int row = 0; row < spreadsheet.getRows(); row++) {
            for (int col = 0; col < spreadsheet.getCols(); col++) {
                Cell cell = spreadsheet.getCell(row, col);
                if (cell.hasError()) {
                    String cellRef = CellReferenceConverter.toCellReference(row, col);
                    errorCells.add(cellRef);
                }
            }
        }

        return errorCells;
    }

    public List<String> getErrorCellsByType(ErrorType errorType) {
        return getErrorReport().get(errorType);
    }

    public boolean hasErrors() {
        return !getAllErrorCells().isEmpty();
    }

    public int getErrorCount() {
        return getAllErrorCells().size();
    }

    public int getErrorCountByType(ErrorType errorType) {
        return getErrorCellsByType(errorType).size();
    }

    public void printDetailedErrorReport() {
        Map<ErrorType, List<String>> errorReport = getErrorReport();
        int totalErrors = getErrorCount();

        System.out.println("\n=== DETAILED ERROR REPORT ===");
        System.out.println("Total Errors: " + totalErrors);

        if (totalErrors == 0) {
            System.out.println("No errors found in the spreadsheet.");
            return;
        }

        for (ErrorType errorType : ErrorType.values()) {
            List<String> errorCells = errorReport.get(errorType);
            if (errorType != ErrorType.NO_ERROR && !errorCells.isEmpty()) {
                printErrorTypeSection(errorType, errorCells);
            }
        }

        printErrorSummary(errorReport);
    }

    private void printErrorTypeSection(ErrorType errorType, List<String> errorCells) {
        System.out.println("\n" + errorType.getDescription() + " (" + errorCells.size() + " cells):");
        System.out.println("-".repeat(50));

        for (String cellRef : errorCells) {
            Cell cell = spreadsheet.getCell(cellRef);
            System.out.printf("  %-6s: %s%n", cellRef, cell.getErrorMessage());

            // نمایش وابستگی‌ها برای خطاهای circular dependency
            if (errorType == ErrorType.CIRCULAR_DEPENDENCY) {
                Set<String> dependencies = cell.getDependencies();
                if (!dependencies.isEmpty()) {
                    System.out.printf("          Dependencies: %s%n", String.join(", ", dependencies));
                }
            }
        }
    }

    private void printErrorSummary(Map<ErrorType, List<String>> errorReport) {
        System.out.println("\n=== ERROR SUMMARY ===");
        for (ErrorType errorType : ErrorType.values()) {
            if (errorType != ErrorType.NO_ERROR) {
                int count = errorReport.get(errorType).size();
                if (count > 0) {
                    System.out.printf("%-20s: %d cells (%.1f%%)%n",
                            errorType.getDescription(),
                            count,
                            (count * 100.0 / getErrorCount())
                    );
                }
            }
        }
    }

    public String getErrorChain(String cellReference) {
        if (!spreadsheet.isValidCellReference(cellReference)) {
            return "Invalid cell reference: " + cellReference;
        }

        Cell cell = spreadsheet.getCell(cellReference);
        if (!cell.hasError()) {
            return "Cell " + cellReference + " has no error.";
        }

        StringBuilder chain = new StringBuilder();
        chain.append("Error chain for ").append(cellReference).append(":\n");
        buildErrorChain(cellReference, chain, new HashSet<>(), 0);

        return chain.toString();
    }

    private void buildErrorChain(String cellRef, StringBuilder chain,
                                 Set<String> visited, int depth) {
        if (visited.contains(cellRef)) {
            chain.append("  ".repeat(depth)).append("↳ [CYCLE] ").append(cellRef).append("\n");
            return;
        }

        visited.add(cellRef);
        Cell cell = spreadsheet.getCell(cellRef);

        chain.append("  ".repeat(depth))
                .append("• ")
                .append(cellRef)
                .append(": ")
                .append(cell.getErrorType().getDescription())
                .append(" - ")
                .append(cell.getErrorMessage())
                .append("\n");

        // پیدا کردن سلول‌هایی که به این سلول وابسته هستند و خطا دارند
        Set<String> dependents = spreadsheet.getDependents(cellRef);
        for (String dependent : dependents) {
            Cell dependentCell = spreadsheet.getCell(dependent);
            if (dependentCell.hasError()) {
                buildErrorChain(dependent, chain, new HashSet<>(visited), depth + 1);
            }
        }
    }
}
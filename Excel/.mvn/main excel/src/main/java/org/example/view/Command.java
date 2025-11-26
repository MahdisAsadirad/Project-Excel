package org.example.view;

import org.example.controller.AutoFillManager;
import org.example.controller.DependencyManager;
import org.example.model.Spreadsheet;
public class Command {
    private Spreadsheet spreadsheet;
    private final SpreadsheetView view;
    private final DependencyManager dependencyManager;
    private final AutoFillManager autoFillManager;

    public Command(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
        this.view = new SpreadsheetView(spreadsheet);
        this.dependencyManager = new DependencyManager(spreadsheet);
        this.autoFillManager = new AutoFillManager(spreadsheet);
    }

    public boolean processCommand(String command) {
        String upperCommand = command.toUpperCase().trim();

        if (command.matches("^[A-Za-z]+\\d+\\s*=.*")) {
            processSet(command);
            return true;
        }

        if (command.matches("^[A-Za-z]+\\d+\\s*=\\s*[^=].*")) {
            processSet(command);
            return true;
        }
        if (upperCommand.startsWith("CLEAR")) {
            processClearCommand(command);
            return true;
        }

        else if (upperCommand.startsWith("FILL")) {
            processFillCommand(command);
            return true;
        }
        else if (upperCommand.startsWith("DETAIL ")) {
            processDetailCommand(command);
            return true;
        }


        switch (upperCommand) {
            case "QUIT":
                System.out.println("Goodbye!");
                return false;
            case "HELP":
                displayHelp();
                break;
            case "SHOW":
                view.displaySpreadsheet();
                break;
            case "STATS":
                System.out.println(view.displayGridStatistics());
                break;
            case "ERRORS":
                view.displayErrors();
                break;
            case "RECALC":
                processRecalcCommand();
                break;
            default:
                System.out.println("Unknown command. Type 'HELP' for available commands.");
        }

        return true;
    }


    private void processSet(String command) {
        int firstEquals = command.indexOf('=');
        if (firstEquals == -1) {
            System.out.println("Invalid assignment format.");
            return;
        }

        String cellRef = command.substring(0, firstEquals).trim().toUpperCase();
        String value = command.substring(firstEquals + 1).trim();

        if (!spreadsheet.isValidCellReference(cellRef)) {
            System.out.println("Invalid cell reference: " + cellRef);
            return;
        }

        try {
            if (value.isEmpty()) {
                spreadsheet.setCellContent(cellRef, "");
            } else {
                spreadsheet.setCellContent(cellRef, preprocessValue(value));
            }

            dependencyManager.recalculateDependencies(cellRef);
            System.out.println("Cell " + cellRef + " set successfully.");
            view.displaySpreadsheet();

        } catch (Exception e) {
            System.out.println("Error setting cell " + cellRef + ": " + e.getMessage());
            view.displaySpreadsheet();
        }
    }

    private String preprocessValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        String trimmed = value.trim();

        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("”") && trimmed.endsWith("”"))) {
            return trimmed;
        }

        if (trimmed.startsWith("=")) {
            return trimmed;
        }

        if (containsAggregateFunction(trimmed)) {
            return "=" + trimmed;
        }

        if (trimmed.contains("!")) {
            return "=" + trimmed;
        }

        if (trimmed.matches("^[+\\-].*") && !trimmed.matches("^[+-]?\\d+(\\.\\d+)?$")) {
            return "=" + trimmed;
        }

        if (looksLikeFormula(trimmed)) {
            return "=" + trimmed;
        }

        try {
            Double.parseDouble(trimmed);
            return trimmed;
        } catch (NumberFormatException e) { }
        return trimmed;
    }

    private boolean containsAggregateFunction(String value) {
        if (value == null) return false;
        String upperValue = value.toUpperCase();
        return upperValue.contains("SUM(") ||
                upperValue.contains("AVG(") ||
                upperValue.contains("MAX(") ||
                upperValue.contains("MIN(") ||
                upperValue.contains("COUNT(");
    }

    private boolean looksLikeFormula(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("”") && value.endsWith("”"))) {
            return false;
        }

        // اگر عدد ساده است، فرمول نیست
        try {
            Double.parseDouble(value);
            return false;
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
        }

        // بررسی کن آیا حاوی عملگرهای ریاضی است
        String coreValue = value.replaceAll("^[+-]", "");
        boolean hasOperators = coreValue.matches(".*[+\\-*/^!].*");

        // بررسی کن آیا حاوی ارجاع سلولی است
        boolean hasCellReferences = value.matches(".*[A-Za-z]\\d+.*");

        // بررسی کن آیا حاوی ثابت‌های PI یا E است
        boolean hasConstants = value.matches(".*\\b(PI|E|pi|e)\\b.*");

        // بررسی کن آیا حاوی توابع تجمعی است
        boolean hasAggregateFunctions = containsAggregateFunction(value);

        // بررسی کن آیا حاوی محدوده است (A1:B5)
        boolean hasRange = value.matches(".*[A-Za-z]\\d+\\s*:\\s*[A-Za-z]\\d+.*");

        boolean looksLikeFormula = hasOperators || hasCellReferences || hasConstants ||
                hasAggregateFunctions || hasRange;

        System.out.println("DEBUG: looksLikeFormula - value: '" + value +
                "', hasOperators: " + hasOperators +
                ", hasCellReferences: " + hasCellReferences +
                ", hasConstants: " + hasConstants +
                ", hasAggregateFunctions: " + hasAggregateFunctions +
                ", hasRange: " + hasRange +
                ", result: " + looksLikeFormula);

        return looksLikeFormula;
    }


    private void displayHelp() {
        System.out.println("\n=== AVAILABLE COMMANDS ===");
        System.out.println("<cell>=<value>         - Set cell content (e.g., A1=5, B1=\"Hello\", C1=A1+B1)");
        System.out.println("FILL (<src>, <range>)     - AutoFill from source cell to target range (e.g., FILL (A1, A1:C1))");
        System.out.println("SHOW                   - Display the spreadsheet");
        System.out.println("DETAIL <cell>          - Show detailed information about a cell");
        System.out.println("STATS                  - Display grid statistics");
        System.out.println("ERRORS                 - Show error report");
        System.out.println("RECALC                 - Recalculate all formulas");
        System.out.println("CLEAR [cell|all]       - Clear specific cell or entire spreadsheet");
        System.out.println("Ctrl+z                   - Undo last action");
        System.out.println("REDO                   - Redo last undone action");
        System.out.println("HISTORY                - Show undo/redo history info");
        System.out.println("CLEARHISTORY           - Clear all history");
        System.out.println("HELP                   - Show this help message");
        System.out.println("EXIT/QUIT              - Exit the program");
    }

    private void processFillCommand(String command) {
        // حذف "FILL" با یا بدون فاصله
        String fillArgs = command.substring(4).trim();
        if (fillArgs.startsWith("(") && fillArgs.endsWith(")")) {
            fillArgs = fillArgs.substring(1, fillArgs.length() - 1); // حذف پرانتزها
        }

        // جدا کردن با کاما
        String[] parts = fillArgs.split("\\s*,\\s*");

        if (parts.length != 2) {
            System.out.println("Invalid FILL command format. Use: FILL(<source>, <range>)");
            return;
        }

        String sourceCell = parts[0].trim().toUpperCase();
        String targetRange = parts[1].trim().toUpperCase();

        try {
            autoFillManager.autoFill(sourceCell, targetRange);
            dependencyManager.recalculateDependencies(sourceCell);

            System.out.println("AutoFill completed successfully.");
            view.displaySpreadsheet();

        } catch (Exception e) {
            System.out.println("Error in AutoFill: " + e.getMessage());
        }
    }



    private void processDetailCommand(String command) {
        String cellReference = command.substring(6).trim().toUpperCase();
        view.displayCellDetails(cellReference);
    }

    private void processClearCommand(String command) {
        String clearArgs = command.substring(5).trim().toUpperCase();

        if (clearArgs.isEmpty() || clearArgs.equals("ALL")) {
            spreadsheet.clear();
            System.out.println("All cells cleared.");
            view.displaySpreadsheet();
        } else {
            try {
                spreadsheet.setCellContent(clearArgs, "");
                dependencyManager.recalculateDependencies(clearArgs);
                System.out.println("Cell " + clearArgs + " cleared.");
                view.displaySpreadsheet();
            } catch (Exception e) {
                System.out.println("Error clearing cell " + clearArgs + ": " + e.getMessage());
            }
        }
    }

    private void processRecalcCommand() {
        try {
            spreadsheet.recalculateAll();
            System.out.println("All formulas recalculated.");
            view.displaySpreadsheet();
        } catch (Exception e) {
            System.out.println("Error during recalculation: " + e.getMessage());
        }
    }

    public void setSpreadsheet(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
    }
}

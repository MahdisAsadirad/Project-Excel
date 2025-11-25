package org.example.view;

import org.example.controller.AutoFillManager;
import org.example.controller.DependencyManager;
import org.example.controller.FormulaEvaluator;
import org.example.model.Spreadsheet;

import java.util.List;
import java.util.Scanner;

public class CommandProcessor {
    private final Spreadsheet spreadsheet;
    private final SpreadsheetView view;
    private final DependencyManager dependencyManager;
    private final AutoFillManager autoFillManager;
    private final FormulaEvaluator formulaEvaluator;
    private final Scanner scanner;

    public CommandProcessor(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
        this.view = new SpreadsheetView(spreadsheet);
        this.dependencyManager = new DependencyManager(spreadsheet);
        this.autoFillManager = new AutoFillManager(spreadsheet);
        this.formulaEvaluator = new FormulaEvaluator(spreadsheet);
        this.scanner = new Scanner(System.in);
    }

    public void startInteractiveMode() {
        System.out.println("*.*.* Spreadsheet *.*.*");
        System.out.println("Type 'HELP' for available commands.");

        boolean running = true;
        while (running) {
            try {
                System.out.print("\n> ");
                String inputLine = scanner.nextLine().trim();

                if (inputLine.isEmpty()) {
                    continue;
                }

                // تقسیم خط ورودی به دستورات مجزا (با ; یا خطوط جدید)
                String[] commands = inputLine.split(";|\\n");

                for (String command : commands) {
                    command = command.trim();
                    if (!command.isEmpty()) {
                        boolean continueRunning = processSingleCommand(command);
                        if (!continueRunning) {
                            running = false;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private boolean processSingleCommand(String command) {
        String upperCommand = command.toUpperCase().trim();

        if ("U".equalsIgnoreCase(command) || "Ctrl+z".equalsIgnoreCase(command)) {
            processUndoCommand();
            return true;
        } else if ("R".equalsIgnoreCase(command) || "ctrl+y".equalsIgnoreCase(command)) {
            processRedoCommand();
            return true;
        }

        if (upperCommand.startsWith("SET ")) {
            processSetCommand(command);
            return true;
        }

        if (command.matches("^[A-Za-z]+\\d+\\s*=\\s*[^=].*")) {
            processDirectAssignment(command);
            return true;
        }

        switch (upperCommand) {

            case "EXIT":
                System.out.println("Goodbye!");
                return false;
            case "Ctrl+z":
                processUndoCommand();
                return true;
            case "REDO":
                processRedoCommand();
                return true;
            case "HISTORY":
                processHistoryCommand();
                return true;
            case "CLEARHISTORY":
                processClearHistoryCommand();
                return true;
            case "HELP":
                displayHelp();
                break;
            case "SHOW":
                view.displaySpreadsheet();
                break;
            case "CLEAR":
                processClearCommand(command);
                break;
            case "STATS":
                view.displayGridStatistics();
                break;
            case "ERRORS":
                view.displayErrors();
                break;
            case "RECALC":
                processRecalcCommand();
                break;
            default:
                if (upperCommand.startsWith("FILL ")) {
                    processFillCommand(command);
                } else if (upperCommand.startsWith("DETAIL ")) {
                    processDetailCommand(command);
                } else {
                    System.out.println("Unknown command: " + command);
                    System.out.println("Type 'HELP' for available commands.");
                }
        }

        return true;
    }

    private void processUndoCommand() {
        try {
            if (spreadsheet.undo()) {
                System.out.println("Undo completed successfully.");
                view.displaySpreadsheet();
            } else {
                System.out.println("Cannot undo - no more actions available.");
                System.out.println(spreadsheet.getHistoryInfo());
            }
        } catch (Exception e) {
            System.out.println("Error during undo: " + e.getMessage());
        }
    }

    private void processRedoCommand() {
        try {
            if (spreadsheet.redo()) {
                System.out.println("Redo completed successfully.");
                view.displaySpreadsheet();
            } else {
                System.out.println("Cannot redo - no actions to redo.");
                System.out.println(spreadsheet.getHistoryInfo());
            }
        } catch (Exception e) {
            System.out.println("Error during redo: " + e.getMessage());
        }
    }

    private void processHistoryCommand() {
        System.out.println("\n=== UNDO/REDO HISTORY ===");
        System.out.println(spreadsheet.getHistoryInfo());

        if (spreadsheet.canUndo()) {
            System.out.println("Use 'UNDO' to revert last action");
        }
        if (spreadsheet.canRedo()) {
            System.out.println("Use 'REDO' to restore undone action");
        }
    }

    private void processClearHistoryCommand() {
        spreadsheet.clearHistory();
        System.out.println("History cleared successfully.");
        System.out.println(spreadsheet.getHistoryInfo());
    }

    public boolean processCommand(String command) {
        String upperCommand = command.toUpperCase().trim();

        // --- تشخیص دستور SET صریح ---
        if (upperCommand.startsWith("SET ")) {
            processSetCommand(command);
            return true;
        }

        // --- تشخیص انتساب مستقیم مانند A1=10 ---
        if (command.matches("^[A-Za-z]+\\d+\\s*=\\s*[^=].*")) {
            processDirectAssignment(command);
            return true;
        }

        // سایر دستورات...
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
            case "CLEAR":
                processClearCommand(command);
                break;
            case "STATS":
                view.displayGridStatistics();
                break;
            case "ERRORS":
                view.displayErrors();
                break;
            case "RECALC":
                processRecalcCommand();
                break;
            case "FILL":
                processFillCommand(command);
                break;
            case "DETAIL":
                processDetailCommand(command);
                break;
            default:
                System.out.println("Unknown command. Type 'HELP' for available commands.");
        }

        return true;
    }

    private void processDirectAssignment(String command) {
        int firstEquals = command.indexOf('=');
        if (firstEquals == -1) {
            System.out.println("Invalid assignment format.");
            return;
        }

        String cellRef = command.substring(0, firstEquals).trim().toUpperCase();
        String value = command.substring(firstEquals + 1).trim();

        System.out.println("DEBUG: Setting " + cellRef + " to: '" + value + "'");

        if (!spreadsheet.isValidCellReference(cellRef)) {
            System.out.println("Invalid cell reference: " + cellRef);
            return;
        }

        try {
            String processedValue = preprocessValue(cellRef, value);

            System.out.println("DEBUG: Processed value: '" + processedValue + "'");

            spreadsheet.setCellContent(cellRef, processedValue);
            dependencyManager.recalculateDependencies(cellRef);
            System.out.println("Cell " + cellRef + " set successfully.");
            view.displaySpreadsheet();
        } catch (Exception e) {
            System.out.println("Error setting cell " + cellRef + ": " + e.getMessage());
            view.displaySpreadsheet();
        }
    }

    private String preprocessValue(String cellRef, String value) {
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

        // اگر متن درون کوتیشن است، فرمول نیست
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("”") && value.endsWith("”"))) {
            return false;
        }

        // اگر عدد ساده است، فرمول نیست
        try {
            Double.parseDouble(value);
            return false;
        } catch (NumberFormatException e) {
            // ادامه بررسی
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
        System.out.println("FILL <src> <range>     - AutoFill from source cell to target range (e.g., FILL A1 A1:C1)");
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

    private void processSetCommand(String command) {
        String assignment = command.substring(4).trim();
        int equalsIndex = assignment.indexOf('=');

        if (equalsIndex == -1) {
            System.out.println("Invalid SET command format. Use: SET <cell>=<value>");
            return;
        }

        String cellReference = assignment.substring(0, equalsIndex).trim().toUpperCase();
        String value = assignment.substring(equalsIndex + 1).trim();

        if (!spreadsheet.isValidCellReference(cellReference)) {
            System.out.println("Invalid cell reference: " + cellReference);
            return;
        }

        try {
            // استفاده از preprocessValue برای SET هم
            String processedValue = preprocessValue(cellReference, value);
            // اعتبارسنجی اولیه مقدار
            if (processedValue.startsWith("\"") && !processedValue.endsWith("\"")) {
                throw new IllegalArgumentException("Unclosed double quotes in text value");
            }

            if (processedValue.startsWith("=") && processedValue.length() == 1) {
                throw new IllegalArgumentException("Formula cannot be empty");
            }

            spreadsheet.setCellContent(cellReference, processedValue);
            dependencyManager.recalculateDependencies(cellReference);

            System.out.println("Cell " + cellReference + " set successfully.");
            view.displaySpreadsheet();

        } catch (Exception e) {
            System.out.println("Error setting cell " + cellReference + ": " + e.getMessage());
            view.displaySpreadsheet();
        }
    }

    private void processFillCommand(String command) {
        String fillArgs = command.substring(5).trim();
        String[] parts = fillArgs.split("\\s+", 2);

        if (parts.length != 2) {
            System.out.println("Invalid FILL command format. Use: FILL <source> <range>");
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
            // پاک کردن سلول خاص
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

    public void processBatchCommands(List<String> commands) {
        for (String command : commands) {
            System.out.println("\n>>> " + command);
            try {
                processCommand(command);
            } catch (Exception e) {
                System.out.println("Error executing command: " + e.getMessage());
            }
        }
    }
}
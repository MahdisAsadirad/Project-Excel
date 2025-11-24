package org.example.excel.view;

import org.example.excel.controller.AutoFillManager;
import org.example.excel.controller.DependencyManager;
import org.example.excel.controller.FormulaEvaluator;
import org.example.excel.model.Spreadsheet;

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
                String input = scanner.nextLine();

                if (input.isEmpty()) {
                    continue;
                }

                running = processCommand(input);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
    }

    public boolean processCommand(String command) {
        String upperCommand = command.toUpperCase().trim();

        // --- NEW: direct assignment like A1=10 ---
        if (command.matches("^[A-Za-z]+\\d+\\s*=.*")) {
            processDirectAssignment(command);
            return true;
        }

        if (upperCommand.equals("QUIT")) {
            System.out.println("Goodbye!");
            return false;

        } else if (upperCommand.equals("HELP")) {
            displayHelp();

        } else if (upperCommand.equals("SHOW")) {
            view.displaySpreadsheet();

        } else if (upperCommand.equals("CLEAR")) {
            processClearCommand(command);

        } else if (upperCommand.equals("STATS")) {
            view.displayGridStatistics();

        } else if (upperCommand.equals("ERRORS")) {
            view.displayErrors();

        }  else if (upperCommand.startsWith("SET ")) {
            processSetCommand(command);

        } else if (upperCommand.startsWith("FILL ")) {
            processFillCommand(command);

        } else if (upperCommand.startsWith("DETAIL ")) {
            processDetailCommand(command);

        } else if (upperCommand.startsWith("RECALC")) {
            processRecalcCommand();

        } else {
            System.out.println("Unknown command. Type 'HELP' for available commands.");
        }

        return true;
    }

    private void processDirectAssignment(String command) {
        String[] parts = command.split("=", 2);

        if (parts.length != 2) {
            System.out.println("Invalid assignment format.");
            return;
        }

        String cellRef = parts[0].trim().toUpperCase();
        String value = parts[1].trim();

        System.out.println("DEBUG: Setting " + cellRef + " to: '" + value + "'");

        if (!spreadsheet.isValidCellReference(cellRef)) {
            System.out.println("Invalid cell reference: " + cellRef);
            return;
        }

        try {
            // استفاده از متد preprocessValue
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

    // متد جدید برای پیش‌پردازش مقدار
    private String preprocessValue(String cellRef, String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        String trimmed = value.trim();
        // --- حالت متن کوتیشن‌دار ---
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) || (trimmed.startsWith("”") && trimmed.endsWith("”") && trimmed.length() >= 2) ) {
            return trimmed.substring(1, trimmed.length() - 1); // حذف " از ابتدا و انتها
        }


        if (trimmed.startsWith("=")) {
            return trimmed; // فرمول واقعی
        }

        // اگر با عملگر شروع شد → به مقدار فعلی اعمال کن
        if (trimmed.matches("^[+\\-*/].*")) {
            double currentValue = 0;
            try {
                currentValue = spreadsheet.getCell(cellRef).getNumericValue();
            } catch (Exception e) {
                currentValue = 0;
            }
            // فرمول = currentValue + rest
            return "=" + currentValue + trimmed;
        }

        // عدد ساده → جایگزین مستقیم
        try {
            Double.parseDouble(trimmed);
            return trimmed;
        } catch (NumberFormatException e) { }

        // بررسی شبیه فرمول
        if (looksLikeFormula(trimmed)) {
            return "=" + trimmed;
        }

        // متن ساده
        return trimmed;
    }



    // متد جدید برای تشخیص فرمول
    private boolean looksLikeFormula(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        // اگر متن درون کوتیشن است، فرمول نیست
        if (value.startsWith("\"") && value.endsWith("\"")) {
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
        boolean hasOperators = value.matches(".*[+\\-*/].*");

        // بررسی کن آیا حاوی ارجاع سلولی است (مثل A1, B2, etc.)
        boolean hasCellReferences = value.matches(".*[A-Za-z]\\d+.*");

        // اگر عملگر دارد یا ارجاع سلولی دارد، احتمالاً فرمول است
        boolean looksLikeFormula = hasOperators || hasCellReferences;

        System.out.println("DEBUG: looksLikeFormula - value: '" + value +
                "', hasOperators: " + hasOperators +
                ", hasCellReferences: " + hasCellReferences +
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
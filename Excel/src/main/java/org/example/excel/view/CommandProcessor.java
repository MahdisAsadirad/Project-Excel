package org.example.excel.view;

import org.example.excel.controller.AutoFillManager;
import org.example.excel.controller.DependencyManager;
import org.example.excel.controller.FormulaEvaluator;
import org.example.excel.exceptions.SpreadsheetException;
import org.example.excel.model.Spreadsheet;

import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

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
        System.out.println("=== Spreadsheet Simulator ===");
        System.out.println("Type 'HELP' for available commands");

        boolean running = true;
        while (running) {
            try {
                System.out.print("\n> ");
                String input = scanner.nextLine().trim();

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
        String upperCommand = command.toUpperCase();

        if (upperCommand.equals("EXIT") || upperCommand.equals("QUIT")) {
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
        } else if (upperCommand.startsWith("SET ")) {
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

    private void displayHelp() {
        System.out.println("\n=== AVAILABLE COMMANDS ===");
        System.out.println("SET <cell>=<value>     - Set cell content (e.g., SET A1=5, SET B1=\"Hello\", SET C1=A1+B1)");
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
        String[] parts = assignment.split("=", 2);

        if (parts.length != 2) {
            System.out.println("Invalid SET command format. Use: SET <cell>=<value>");
            return;
        }

        String cellReference = parts[0].trim().toUpperCase();
        String value = parts[1].trim();

        try {
            // ذخیره وابستگی‌های قبلی برای تشخیص تغییرات
            Set<String> oldDependencies = new HashSet<>(
                    spreadsheet.getDependencies(cellReference)
            );

            // تنظیم محتوای سلول
            spreadsheet.setCellContent(cellReference, value);

            // محاسبه مجدد وابستگی‌ها
            dependencyManager.recalculateDependencies(cellReference);

            System.out.println("Cell " + cellReference + " set successfully.");
            view.displaySpreadsheet();

        } catch (SpreadsheetException e) {
            System.out.println("Error setting cell " + cellReference + ": " + e.getMessage());
            view.displaySpreadsheet(); // نمایش وضعیت حتی با خطا
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
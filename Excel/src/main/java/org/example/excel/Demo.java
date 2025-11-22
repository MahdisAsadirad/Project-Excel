package org.example.excel;


import org.example.excel.model.Spreadsheet;
import org.example.excel.view.CommandProcessor;

import java.util.Arrays;
import java.util.List;

public class Demo {
    public static void main(String[] args) {
        System.out.println("=== Spreadsheet Simulator Demo ===");

        Spreadsheet spreadsheet = new Spreadsheet(5, 5);
        CommandProcessor processor = new CommandProcessor(spreadsheet);

        runCorrectedDemo(processor);
    }

    private static void runCorrectedDemo(CommandProcessor processor) {
        System.out.println("\n--- Demo 1: Basic Operations (Corrected) ---");
        List<String> demo1Commands = Arrays.asList(
                "SET A1=10",
                "SET B1=20",
                "SET C1=30",
                "SET D1=\"Total:\"",
                "SET E1=A1+B1+C1", 
                "SHOW",
                "STATS"
        );
        processor.processBatchCommands(demo1Commands);

        System.out.println("\n--- Demo 2: Formulas and Dependencies (Corrected) ---");
        List<String> demo2Commands = Arrays.asList(
                "SET A2=5",
                "SET B2=3",
                "SET C2=A2*B2",  // ✅ اضافه کردن =
                "SET D2=C2+10",  // ✅ اضافه کردن =
                "SET E2=D2/2",   // ✅ اضافه کردن =
                "SHOW",
                "DETAIL C2",
                "DETAIL E2"
        );
        processor.processBatchCommands(demo2Commands);

        System.out.println("\n--- Demo 3: AutoFill Feature (Corrected) ---");
        List<String> demo3Commands = Arrays.asList(
                "SET A3=1",
                "SET B3=2",
                "SET C3=A3+B3",  // ✅ اضافه کردن =
                "FILL A3 A3:A5",
                "FILL B3 B3:B5",
                "FILL C3 C3:C5",
                "SHOW"
        );
        processor.processBatchCommands(demo3Commands);

        System.out.println("\n--- Demo 4: Error Handling ---");
        List<String> demo4Commands = Arrays.asList(
                "SET A4=10",
                "SET B4=0",
                "SET C4=A4/B4",
                "SET D4=C4+5",
                "SHOW",
                "ERRORS",
                "DETAIL C4",
                "DETAIL D4"
        );
        processor.processBatchCommands(demo4Commands);

        System.out.println("\n--- Demo 5: Circular Dependency ---");
        List<String> demo5Commands = Arrays.asList(
                "SET A5=B5+1",
                "SET B5=A5+1",
                "SHOW",
                "ERRORS"
        );
        processor.processBatchCommands(demo5Commands);

        System.out.println("\n--- Demo 6: Complex Formulas ---");
        List<String> demo6Commands = Arrays.asList(
                "SET A1=2",
                "SET B1=3",
                "SET C1=4",
                "SET D1=(A1+B1)*C1",
                "SET E1=D1/2+PI",
                "SHOW",
                "DETAIL D1",
                "DETAIL E1"
        );
        processor.processBatchCommands(demo6Commands);

        System.out.println("\n--- Demo Completed ---");
    }
}
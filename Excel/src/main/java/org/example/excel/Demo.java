package org.example.excel;

import org.example.excel.model.Spreadsheet;
import org.example.excel.view.CommandProcessor;
import org.example.excel.view.SpreadsheetView;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Demo {
    public static void main(String[] args) {
        System.out.println("*-*-* Spreadsheet Simulator Demo *-*-*");
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
                "SET E1==A1+B1+C1",  // ✅ با = اضافه
                "SHOW",
                "STATS"
        );
        processor.processBatchCommands(demo1Commands);

        System.out.println("\n--- Demo 2: Formulas and Dependencies (Corrected) ---");
        List<String> demo2Commands = Arrays.asList(
                "SET A2=5",
                "SET B2=3",
                "SET C2==A2*B2",  // ✅ با = اضافه
                "SET D2==C2+10",   // ✅ با = اضافه
                "SET E2==D2/2",    // ✅ با = اضافه
                "SHOW",
                "DETAIL C2",
                "DETAIL E2"
        );
        processor.processBatchCommands(demo2Commands);

        System.out.println("\n--- Demo 3: AutoFill Feature (Corrected) ---");
        List<String> demo3Commands = Arrays.asList(
                "SET A3=1",
                "SET B3=2",
                "SET C3==A3+B3",  // ✅ با = اضافه
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
                "SET C4==A4/B4", // ✅ با = اضافه - خطای تقسیم بر صفر
                "SET D4==C4+5",   // ✅ با = اضافه - خطای وابستگی
                "SHOW",
                "ERRORS",
                "DETAIL C4",
                "DETAIL D4"
        );
        processor.processBatchCommands(demo4Commands);

        System.out.println("\n--- Demo 5: Text and Mixed Content ---");
        List<String> demo5Commands = Arrays.asList(
                "SET A1=\"Hello\"",
                "SET B1=\"World\"",
                "SET C1==A1&\" \"&B1", // ✅ با = اضافه - الحاق متن
                "SHOW",
                "DETAIL C1"
        );
        processor.processBatchCommands(demo5Commands);

        System.out.println("\n--- Demo 6: Complex Formulas ---");
        List<String> demo6Commands = Arrays.asList(
                "SET A5=2",
                "SET B5=3",
                "SET C5=4",
                "SET D5==(A5+B5)*C5", // ✅ با = اضافه
                "SET E5==D5/2+PI",    // ✅ با = اضافه
                "SHOW",
                "DETAIL D5",
                "DETAIL E5"
        );
        processor.processBatchCommands(demo6Commands);

        System.out.println("\n--- Demo Completed ---");
    }
}

package org.example.excel;

import org.example.excel.model.Spreadsheet;
import org.example.excel.view.CommandProcessor;
import org.example.excel.view.SpreadsheetView;

import java.util.Scanner;

public class Demo {
    public static void main(String[] args) {
        System.out.println("Welcome to Excel ^-^");
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of columns: ");
        int columns = sc.nextInt();
        System.out.print("Enter number of rows: ");
        int rows = sc.nextInt();
        Spreadsheet spreadsheet = new Spreadsheet(rows, columns);
        CommandProcessor processor = new CommandProcessor(spreadsheet);

        SpreadsheetView spreadsheetView= new SpreadsheetView(spreadsheet);
        spreadsheetView.displaySpreadsheet();
        processor.startInteractiveMode();
    }
}
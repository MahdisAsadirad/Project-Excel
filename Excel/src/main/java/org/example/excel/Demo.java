
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
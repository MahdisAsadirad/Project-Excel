package org.example;

import org.example.controller.GUICommandProcessor;
import org.example.model.Spreadsheet;
import org.example.view.SpreadsheetGUI;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            try {
                String rowsStr = JOptionPane.showInputDialog("Enter number of rows:", "10");
                String colsStr = JOptionPane.showInputDialog("Enter number of columns:", "10");

                if (rowsStr == null || colsStr == null) {
                    return;
                }

                int rows = Integer.parseInt(rowsStr);
                int cols = Integer.parseInt(colsStr);

                Spreadsheet spreadsheet = new Spreadsheet(rows, cols);
                GUICommandProcessor processor = new GUICommandProcessor(spreadsheet);

                new SpreadsheetGUI(spreadsheet, processor);

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null,
                        "Invalid input! Please enter valid numbers.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Error starting application: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
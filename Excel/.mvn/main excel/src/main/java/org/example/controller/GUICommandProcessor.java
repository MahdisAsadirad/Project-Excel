package org.example.controller;


import org.example.model.Spreadsheet;
import org.example.view.CommandProcessor;

public class GUICommandProcessor extends CommandProcessor {
    public GUICommandProcessor(Spreadsheet spreadsheet) {
        super(spreadsheet);
    }

    @Override
    public void startInteractiveMode() {
        System.out.println("GUI Mode Activated");
    }

    @Override
    public boolean processCommand(String command) {
        try {
            return super.processCommand(command);
        } catch (Exception e) {
            throw new RuntimeException("Error processing command: " + command, e);
        }
    }

    public String getCommandOutput(String command) {
        try {
            processCommand(command);
            return "Command executed successfully: " + command;
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
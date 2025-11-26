package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import org.example.model.Spreadsheet;
import org.example.view.CommandProcessor;
import org.example.view.SpreadsheetGUIController;

import java.util.Optional;

public class SpreadsheetFXApplication extends Application {

    private Spreadsheet spreadsheet;
    private CommandProcessor commandProcessor;

    @Override
    public void start(Stage primaryStage) throws Exception {
        int cols = getGridDimension("Enter number of columns:", "0");
        int rows = getGridDimension("Enter number of rows:", "0");

        this.spreadsheet = new Spreadsheet(rows, cols);
        this.commandProcessor = new CommandProcessor(spreadsheet);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/SpreadsheetGUI.fxml"));
        loader.setControllerFactory(param -> new SpreadsheetGUIController(spreadsheet, commandProcessor));

        Parent root = loader.load();

        primaryStage.setTitle("Excel Spreadsheet(" + cols + "x" + rows + ")");
        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    private int getGridDimension(String message, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle("Spreadsheet Configuration");
        dialog.setHeaderText("Set Spreadsheet Dimensions");
        dialog.setContentText(message);

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            try {
                int value = Integer.parseInt(result.get());
                if (value > 0 && value <= 26) { // محدودیت منطقی
                    return value;
                } else {
                    showErrorDialog("Invalid dimension! Please enter a number between 1 and 26.");
                    return getGridDimension(message, defaultValue);
                }
            } catch (NumberFormatException e) {
                showErrorDialog("Invalid number format! Please enter a valid integer.");
                return getGridDimension(message, defaultValue);
            }
        } else {
            System.exit(0);
            return -1;
        }
    }

    private void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Input Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
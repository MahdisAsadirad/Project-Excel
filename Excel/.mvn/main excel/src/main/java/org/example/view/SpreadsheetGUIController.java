package org.example.view;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.model.Cell;
import org.example.model.CellType;
import org.example.model.Spreadsheet;
import org.example.utils.CellReferenceConverter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.net.URL;
import java.util.ResourceBundle;

public class SpreadsheetGUIController implements Initializable {

    private Spreadsheet spreadsheet;
    private final CommandProcessor commandProcessor;

    @FXML private TableView<ObservableList<String>> spreadsheetTable;
    @FXML private TextField commandField;
    @FXML private TextArea outputArea;
    @FXML private Button executeButton;
    @FXML private Button undoButton;
    @FXML private Button redoButton;
    @FXML private Button clearButton;

    private final ObservableList<ObservableList<String>> tableData;

    public SpreadsheetGUIController(Spreadsheet spreadsheet, CommandProcessor commandProcessor) {
        this.spreadsheet = spreadsheet;
        this.commandProcessor = commandProcessor;
        this.tableData = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupEventHandlers();
        setupKeyboardShortcuts();
        updateButtonStates();
        refreshTable();

        outputArea.appendText("Spreadsheet initialized: " +
                spreadsheet.getRows() + " rows x " + spreadsheet.getCols() + " columns\n");
    }

    private void setupTable() {
        spreadsheetTable.setEditable(false);
        spreadsheetTable.setFixedCellSize(25);

        spreadsheetTable.getColumns().clear();

        for (int i = 0; i < spreadsheet.getCols(); i++) {
            final int columnIndex = i;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(
                    CellReferenceConverter.getColumnName(i)
            );

            column.setCellValueFactory(param -> {
                ObservableList<String> row = param.getValue();
                return new SimpleStringProperty(row.get(columnIndex));
            });

            column.setCellFactory(_ -> new SpreadsheetTableCell(columnIndex));

            column.setPrefWidth(100);
            column.setResizable(true);
            spreadsheetTable.getColumns().add(column);
        }

        refreshTableData();

        spreadsheetTable.setRowFactory(tv -> {
            TableRow<ObservableList<String>> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    editSelectedCell();
                }
            });
            return row;
        });

        spreadsheetTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateButtonStates());
    }

    private void refreshTableData() {
        tableData.clear();

        for (int row = 0; row < spreadsheet.getRows(); row++) {
            ObservableList<String> rowData = FXCollections.observableArrayList();
            for (int col = 0; col < spreadsheet.getCols(); col++) {
                try {
                    String cellRef = CellReferenceConverter.toCellReference(row, col);
                    Cell cell = spreadsheet.getCell(cellRef);
                    rowData.add(cell.getDisplayValue());
                } catch (Exception e) {
                    rowData.add("#ERR!");
                }
            }
            tableData.add(rowData);
        }

        spreadsheetTable.setItems(tableData);
    }

    private void setupEventHandlers() {
        executeButton.setOnAction(e -> executeCommand());
        commandField.setOnAction(e -> executeCommand());
    }

    private void setupKeyboardShortcuts() {
        Scene scene = spreadsheetTable.getScene();
        if (scene != null) {
            // Undo: Ctrl+Z
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN),
                    this::undo
            );

            // Redo: Ctrl+Y
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN),
                    this::redo
            );

            // Execute: Ctrl+Enter
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
                    this::executeCommand
            );

            // Clear: Delete
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.DELETE),
                    this::clearSelectedCell
            );
        }
    }

    @FXML
    private void executeCommand() {
        String command = commandField.getText().trim();
        if (command.isEmpty()) return;

        try {
            outputArea.appendText("> " + command + "\n");

            boolean continueRunning = commandProcessor.processCommand(command);

            if (!continueRunning) {
                exitApplication();
                return;
            }

            refreshTable();
            updateButtonStates();
            outputArea.appendText("Command executed successfully.\n\n");

            commandField.setText("");
            commandField.requestFocus();

        } catch (Exception ex) {
            outputArea.appendText("ERROR: " + ex.getMessage() + "\n\n");
            ex.printStackTrace();
        }

        outputArea.setScrollTop(Double.MAX_VALUE);
    }

    @FXML
    private void undo() {
        try {
            if (spreadsheet.undo()) {
                refreshTable();
                updateButtonStates();
                outputArea.appendText("Undo performed successfully.\n\n");
            } else {
                outputArea.appendText("Cannot undo - no more actions available.\n\n");
            }
        } catch (Exception ex) {
            outputArea.appendText("ERROR during undo: " + ex.getMessage() + "\n\n");
        }
    }

    @FXML
    private void redo() {
        try {
            if (spreadsheet.redo()) {
                refreshTable();
                updateButtonStates();
                outputArea.appendText("Redo performed successfully.\n\n");
            } else {
                outputArea.appendText("Cannot redo - no actions to redo.\n\n");
            }
        } catch (Exception ex) {
            outputArea.appendText("ERROR during redo: " + ex.getMessage() + "\n\n");
        }
    }

    @FXML
    private void editSelectedCell() {
        int row = spreadsheetTable.getSelectionModel().getSelectedIndex();
        if (row < 0) return;

        ObservableList<TablePosition> selectedCells = spreadsheetTable.getSelectionModel().getSelectedCells();
        if (selectedCells.isEmpty()) return;

        int col = selectedCells.get(0).getColumn();

        if (row >= 0 && col >= 0) {
            String cellRef = CellReferenceConverter.toCellReference(row, col);
            Cell cell = spreadsheet.getCell(cellRef);

            String currentValue = cell.getRawContent();
            if (currentValue.isEmpty() && cell.getCellType() != org.example.model.CellType.FORMULA) {
                currentValue = "";
            }

            TextInputDialog dialog = new TextInputDialog(currentValue);
            dialog.setTitle("Edit Cell");
            dialog.setHeaderText("Edit cell " + cellRef);
            dialog.setContentText("Value:");

            dialog.showAndWait().ifPresent(newValue -> {
                executeCommand(cellRef + "=" + newValue);
            });
        }
    }

    @FXML
    private void clearSelectedCell() {
        int row = spreadsheetTable.getSelectionModel().getSelectedIndex();
        if (row < 0) return;

        ObservableList<TablePosition> selectedCells = spreadsheetTable.getSelectionModel().getSelectedCells();
        if (selectedCells.isEmpty()) return;

        int col = selectedCells.get(0).getColumn();

        if (row >= 0 && col >= 0) {
            String cellRef = CellReferenceConverter.toCellReference(row, col);
            executeCommand("CLEAR " + cellRef);
        }
    }

    private void executeCommand(String command) {
        commandField.setText(command);
        executeCommand();
    }

    @FXML
    private void updateButtonStates() {
        undoButton.setDisable(!spreadsheet.canUndo());
        redoButton.setDisable(!spreadsheet.canRedo());
        clearButton.setDisable(spreadsheetTable.getSelectionModel().getSelectedCells().isEmpty());
    }

    @FXML
    private void createNewSpreadsheet() {
        try {
            TextInputDialog rowsDialog = new TextInputDialog(String.valueOf(spreadsheet.getRows()));
            rowsDialog.setTitle("New Spreadsheet");
            rowsDialog.setHeaderText("Enter number of rows:");

            TextInputDialog colsDialog = new TextInputDialog(String.valueOf(spreadsheet.getCols()));
            colsDialog.setTitle("New Spreadsheet");
            colsDialog.setHeaderText("Enter number of columns:");

            String rowsStr = rowsDialog.showAndWait().orElse(null);
            String colsStr = colsDialog.showAndWait().orElse(null);

            if (rowsStr != null && colsStr != null) {
                int rows = Integer.parseInt(rowsStr);
                int cols = Integer.parseInt(colsStr);

                Spreadsheet newSheet = new Spreadsheet(rows, cols);
                this.spreadsheet = newSheet;
                this.commandProcessor.setSpreadsheet(newSheet);

                setupTable();
                refreshTable();
                outputArea.appendText("New spreadsheet created: " + rows + "x" + cols + "\n");

                Stage stage = (Stage) commandField.getScene().getWindow();
                stage.setTitle("Excel Spreadsheet (" + rows + "x" + cols + ")");
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid dimensions!", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void showStatistics() {
        int totalCells = spreadsheet.getRows() * spreadsheet.getCols();
        int formulaCells = 0;
        int numberCells = 0;
        int textCells = 0;
        int errorCells = 0;
        int emptyCells = 0;

        for (int row = 0; row < spreadsheet.getRows(); row++) {
            for (int col = 0; col < spreadsheet.getCols(); col++) {
                try {
                    String cellRef = CellReferenceConverter.toCellReference(row, col);
                    Cell cell = spreadsheet.getCell(cellRef);

                    if (cell.hasError()) {
                        errorCells++;
                    } else if (cell.getRawContent().isEmpty()) {
                        emptyCells++;
                    } else {
                        switch (cell.getCellType()) {
                            case FORMULA: formulaCells++; break;
                            case NUMBER: numberCells++; break;
                            case TEXT: textCells++; break;
                        }
                    }
                } catch (Exception e) {
                    errorCells++;
                }
            }
        }

        String statsText = String.format("""
            === SPREADSHEET STATISTICS ===
            
            Dimensions: %d rows x %d columns
            Total Cells: %d
            
            Cell Type Distribution:
            • Empty Cells: %d (%.1f%%)
            • Number Cells: %d (%.1f%%)
            • Text Cells: %d (%.1f%%)
            • Formula Cells: %d (%.1f%%)
            • Error Cells: %d (%.1f%%)
            
            Undo/Redo Stack:
            • Undo available: %s
            • Redo available: %s
            """,
                spreadsheet.getRows(), spreadsheet.getCols(), totalCells,
                emptyCells, (emptyCells * 100.0 / totalCells),
                numberCells, (numberCells * 100.0 / totalCells),
                textCells, (textCells * 100.0 / totalCells),
                formulaCells, (formulaCells * 100.0 / totalCells),
                errorCells, (errorCells * 100.0 / totalCells),
                spreadsheet.canUndo() ? "Yes" : "No",
                spreadsheet.canRedo() ? "Yes" : "No"
        );

        TextArea statsArea = new TextArea(statsText);
        statsArea.setEditable(false);
        statsArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        ScrollPane scrollPane = new ScrollPane(statsArea);
        scrollPane.setPrefSize(500, 400);

        Stage statsStage = new Stage();
        statsStage.setTitle("Spreadsheet Statistics");
        statsStage.setScene(new Scene(scrollPane));
        statsStage.show();
    }

    @FXML
    private void showAbout() {
        showAlert("About",
                "Excel Spreadsheet Simulator\n" +
                        "Version 1.0\n" +
                        "Built with JavaFX\n" +
                        "Dimensions: " + spreadsheet.getRows() + "x" + spreadsheet.getCols() + "\n" +
                        "© 2024 Excel Team",
                Alert.AlertType.INFORMATION);
    }

    @FXML
    private void showHelp() {
        String helpText = """
        === SPREADSHEET HELP ===
        
        BASIC COMMANDS:
        • Click on a cell and type in command line:
          A1=5           - Set number
          B1="Hello"     - Set text  
          C1=A1+B1       - Set formula
          CLEAR A1       - Clear cell
        
        KEYBOARD SHORTCUTS:
        • Ctrl+Z         - Undo
        • Ctrl+Y         - Redo
        • Delete         - Clear selected cell
        • Double-click   - Edit cell
        • Ctrl+Enter     - Execute command
        
        FORMULAS:
        • Basic: =A1+B1, =A1*2, =SUM(A1:A5)
        • Functions: SUM, AVG, MAX, MIN, COUNT
        • Constants: PI, E
        
        AUTO FILL:
        • FILL A1 A1:C1  - Copy A1 to B1, C1
        
        CURRENT GRID: """ + spreadsheet.getRows() + " rows x " + spreadsheet.getCols() + " columns";

        TextArea helpArea = new TextArea(helpText);
        helpArea.setEditable(false);
        helpArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        ScrollPane scrollPane = new ScrollPane(helpArea);
        scrollPane.setPrefSize(550, 450);

        Stage helpStage = new Stage();
        helpStage.setTitle("Help - " + spreadsheet.getRows() + "x" + spreadsheet.getCols() + " Spreadsheet");
        helpStage.setScene(new Scene(scrollPane));
        helpStage.show();
    }

    @FXML
    private void refreshTable() {
        refreshTableData();
        outputArea.appendText("Table refreshed at " + java.time.LocalTime.now() + "\n");
    }

    @FXML
    private void exitApplication() {
        Stage stage = (Stage) commandField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void insertSumFormula() {
        commandField.setText("=SUM(");
        commandField.requestFocus();
        commandField.end();
    }

    @FXML
    private void insertAvgFormula() {
        commandField.setText("=AVG(");
        commandField.requestFocus();
        commandField.end();
    }

    @FXML
    private void insertFormula() {
        commandField.setText("=");
        commandField.requestFocus();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private class SpreadsheetTableCell extends TableCell<ObservableList<String>, String> {
        private final int columnIndex;

        public SpreadsheetTableCell(int columnIndex) {
            this.columnIndex = columnIndex;
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || getTableRow() == null) {
                setText(null);
                setStyle("");
                return;
            }

            int rowIndex = getIndex();
            setText(item);

            try {
                String cellRef = CellReferenceConverter.toCellReference(rowIndex, columnIndex);
                Cell cell = spreadsheet.getCell(cellRef);

                if (cell.hasError()) {
                    setStyle("-fx-background-color: #ffc8c8; -fx-text-fill: red; -fx-alignment: center;");
                } else if (cell.getCellType() == CellType.FORMULA) {
                    setStyle("-fx-background-color: #dcf0ff; -fx-text-fill: blue; -fx-alignment: center;");
                } else if (cell.getCellType() == CellType.TEXT) {
                    setStyle("-fx-background-color: #ffffdc; -fx-text-fill: black; -fx-alignment: center;");
                } else if (cell.getCellType() == CellType.NUMBER) {
                    setStyle("-fx-background-color: #dcffdc; -fx-text-fill: black; -fx-alignment: center;");
                } else {
                    setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-alignment: center;");
                }

            } catch (Exception e) {
                setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-alignment: center;");
            }

            // Border
            setStyle(getStyle() + " -fx-border-color: lightgray; -fx-border-width: 0.5;");
        }
    }
}

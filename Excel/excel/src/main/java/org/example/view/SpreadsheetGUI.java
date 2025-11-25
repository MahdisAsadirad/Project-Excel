package org.example.view;

import org.example.model.Cell;
import org.example.model.Spreadsheet;
import org.example.utils.CellReferenceConverter;
import org.example.model.CellType;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;

public class SpreadsheetGUI extends JFrame {
    private final Spreadsheet spreadsheet;
    private final CommandProcessor commandProcessor;
    private SpreadsheetTableModel tableModel;
    private JTable table;
    private JTextField commandField;
    private JTextArea outputArea;
    private JButton executeButton;
    private JButton undoButton;
    private JButton redoButton;
    private JButton clearButton;
    private JButton fillButton;

    public SpreadsheetGUI(Spreadsheet spreadsheet, CommandProcessor commandProcessor) {
        this.spreadsheet = spreadsheet;
        this.commandProcessor = commandProcessor;

        initializeGUI();
        setupEventHandlers();
    }

    private void createTable() {
        tableModel = new SpreadsheetTableModel();
        table = new JTable(tableModel);

        table.setRowHeight(25);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(100);
        }

        table.setDefaultRenderer(Object.class, new SpreadsheetCellRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel commandPanel = new JPanel(new BorderLayout(5, 5));
        commandPanel.add(new JLabel("Command: "), BorderLayout.WEST);

        commandField = new JTextField();
        commandField.setFont(new Font("Monospaced", Font.PLAIN, 14));
        commandPanel.add(commandField, BorderLayout.CENTER);

        executeButton = new JButton("Execute");
        commandPanel.add(executeButton, BorderLayout.EAST);

        controlPanel.add(commandPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        undoButton = new JButton("Undo (Ctrl+z)");
        redoButton = new JButton("Redo (Ctrl+y)");
        clearButton = new JButton("Clear Cell");
        fillButton = new JButton("Auto Fill");

        buttonPanel.add(undoButton);
        buttonPanel.add(redoButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(fillButton);
        buttonPanel.add(Box.createHorizontalStrut(20));

        JButton sumButton = new JButton("SUM");
        JButton avgButton = new JButton("AVG");
        JButton formulaButton = new JButton("Insert Formula");

        buttonPanel.add(sumButton);
        buttonPanel.add(avgButton);
        buttonPanel.add(formulaButton);

        controlPanel.add(buttonPanel, BorderLayout.CENTER);

        add(controlPanel, BorderLayout.NORTH);
    }

    private void createOutputArea() {
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output & Messages"));

        outputArea = new JTextArea(8, 80);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setBackground(new Color(240, 240, 240));

        JScrollPane scrollPane = new JScrollPane(outputArea);
        outputPanel.add(scrollPane, BorderLayout.CENTER);

        add(outputPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        executeButton.addActionListener(e -> executeCommand());
        commandField.addActionListener(e -> executeCommand());

        undoButton.addActionListener(e -> undo());
        redoButton.addActionListener(e -> redo());
        clearButton.addActionListener(e -> clearSelectedCell());
        fillButton.addActionListener(e -> showFillDialog());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedCell();
                }
            }
        });

        setupKeyboardShortcuts();

        updateButtonStates();
    }

    private void showFillDialog() {
        int selectedRow = table.getSelectedRow();
        int selectedCol = table.getSelectedColumn();

        if (selectedRow < 0 || selectedCol < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a source cell first!",
                    "No Cell Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sourceCell = CellReferenceConverter.toCellReference(selectedRow, selectedCol);

        JPanel fillPanel = new JPanel(new GridLayout(3, 2, 5, 5));

        fillPanel.add(new JLabel("Source Cell:"));
        JTextField sourceField = new JTextField(sourceCell);
        sourceField.setEditable(false);
        fillPanel.add(sourceField);

        fillPanel.add(new JLabel("Target Range:"));
        JTextField rangeField = new JTextField();
        fillPanel.add(rangeField);

        fillPanel.add(new JLabel("Example: A1:C1"));
        JButton helpButton = new JButton("Help");
        fillPanel.add(helpButton);

        helpButton.addActionListener(e -> showFillHelp());

        int result = JOptionPane.showConfirmDialog(this,
                fillPanel,
                "Auto Fill",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String targetRange = rangeField.getText().trim();
            if (!targetRange.isEmpty()) {
                executeCommand("FILL " + sourceCell + " " + targetRange);
            }
        }
    }

    private void showFillHelp() {
        String helpText = """
        AUTO FILL HELP:
        
        • Select a source cell first
        • Enter target range in format: StartCell:EndCell
        
        EXAMPLES:
        A1:A5    - Fill A1 to A5
        A1:C1    - Fill A1 to C1 (horizontal)
        A1:C3    - Fill 3x3 block
        
        FEATURES:
        • Copies values, text, and formulas
        • Adjusts cell references automatically
        • Works with relative references
        """;

        JOptionPane.showMessageDialog(this, helpText, "Auto Fill Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setupKeyboardShortcuts() {
        InputMap inputMap = commandField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = commandField.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "execute");
        actionMap.put("execute", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeCommand();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "clear");
        actionMap.put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearSelectedCell();
            }
        });
    }

    private void executeCommand() {
        String command = commandField.getText().trim();
        if (command.isEmpty()) return;

        try {
            outputArea.append("> " + command + "\n");

            boolean continueRunning = commandProcessor.processCommand(command);

            if (!continueRunning) {
                dispose();
                return;
            }

            tableModel.fireTableDataChanged();
            updateButtonStates();
            outputArea.append("Command executed successfully.\n\n");

            commandField.setText("");
            commandField.requestFocus();

        } catch (Exception ex) {
            outputArea.append("ERROR: " + ex.getMessage() + "\n\n");
            ex.printStackTrace();
        }

        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void undo() {
        try {
            if (spreadsheet.undo()) {
                tableModel.fireTableDataChanged();
                updateButtonStates();
                outputArea.append("Undo performed successfully.\n\n");
            } else {
                outputArea.append("Cannot undo - no more actions available.\n\n");
            }
        } catch (Exception ex) {
            outputArea.append("ERROR during undo: " + ex.getMessage() + "\n\n");
        }
    }

    private void redo() {
        try {
            if (spreadsheet.redo()) {
                tableModel.fireTableDataChanged();
                updateButtonStates();
                outputArea.append("Redo performed successfully.\n\n");
            } else {
                outputArea.append("Cannot redo - no actions to redo.\n\n");
            }
        } catch (Exception ex) {
            outputArea.append("ERROR during redo: " + ex.getMessage() + "\n\n");
        }
    }

    private void editSelectedCell() {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();

        if (row >= 0 && col >= 0) {
            String cellRef = CellReferenceConverter.toCellReference(row, col);
            Cell cell = spreadsheet.getCell(cellRef);

            String currentValue = cell.getRawContent();
            if (currentValue.isEmpty() && cell.getCellType() != CellType.FORMULA) {
                currentValue = "";
            }

            String newValue = JOptionPane.showInputDialog(
                    this,
                    "Edit cell " + cellRef + ":",
                    currentValue
            );

            if (newValue != null) {
                executeCommand(cellRef + "=" + newValue);
            }
        }
    }

    private void clearSelectedCell() {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();

        if (row >= 0 && col >= 0) {
            String cellRef = CellReferenceConverter.toCellReference(row, col);
            executeCommand("CLEAR " + cellRef);
        }
    }

    private void executeCommand(String command) {
        commandField.setText(command);
        executeCommand();
    }

    private void updateButtonStates() {
        undoButton.setEnabled(spreadsheet.canUndo());
        redoButton.setEnabled(spreadsheet.canRedo());
        clearButton.setEnabled(table.getSelectedRow() >= 0);
    }

    // Table Model برای نمایش داده‌ها
    private class SpreadsheetTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return spreadsheet.getRows();
        }

        @Override
        public int getColumnCount() {
            return spreadsheet.getCols();
        }

        @Override
        public String getColumnName(int column) {
            return CellReferenceConverter.getColumnName(column);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            try {
                String cellRef = CellReferenceConverter.toCellReference(rowIndex, columnIndex);
                Cell cell = spreadsheet.getCell(cellRef);
                return cell.toString();
            } catch (Exception e) {
                return "#ERR!";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private class SpreadsheetCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            try {
                String cellRef = CellReferenceConverter.toCellReference(row, column);
                Cell cell = spreadsheet.getCell(cellRef);

                if (cell.hasError()) {
                    c.setBackground(new Color(255, 200, 200)); // قرمز برای خطا
                    c.setForeground(Color.RED);
                } else if (cell.getCellType() == CellType.FORMULA) {
                    c.setBackground(new Color(220, 240, 255)); // آبی برای فرمول
                    c.setForeground(Color.BLUE);
                } else if (cell.getCellType() == CellType.TEXT) {
                    c.setBackground(new Color(255, 255, 220)); // زرد برای متن
                    c.setForeground(Color.BLACK);
                } else if (cell.getCellType() == CellType.NUMBER) {
                    c.setBackground(new Color(220, 255, 220)); // سبز برای اعداد
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }

                if (isSelected) {
                    c.setBackground(c.getBackground().darker());
                }

            } catch (Exception e) {
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
            }

            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

            return c;
        }
    }
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem exitItem = new JMenuItem("Exit");

        newItem.addActionListener(e -> createNewSpreadsheet());
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(newItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo");
        JMenuItem redoItem = new JMenuItem("Redo");
        JMenuItem clearItem = new JMenuItem("Clear Selection");
        JMenuItem fillItem = new JMenuItem("Auto Fill");

        undoItem.addActionListener(e -> undo());
        redoItem.addActionListener(e -> redo());
        clearItem.addActionListener(e -> clearSelectedCell());
        fillItem.addActionListener(e -> showFillDialog());

        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(clearItem);
        editMenu.add(fillItem);

        JMenu viewMenu = new JMenu("View");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        JMenuItem statsItem = new JMenuItem("Show Statistics");

        refreshItem.addActionListener(e -> tableModel.fireTableDataChanged());
        statsItem.addActionListener(e -> showStatistics());

        viewMenu.add(refreshItem);
        viewMenu.add(statsItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        JMenuItem helpItem = new JMenuItem("Show Help");

        aboutItem.addActionListener(e -> showAbout());
        helpItem.addActionListener(e -> showHelp());

        helpMenu.add(helpItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutItem);

        // اضافه کردن منوها
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void createNewSpreadsheet() {
        try {
            String rowsStr = JOptionPane.showInputDialog(this, "Enter number of rows:", "10");
            String colsStr = JOptionPane.showInputDialog(this, "Enter number of columns:", "10");

            if (rowsStr != null && colsStr != null) {
                int rows = Integer.parseInt(rowsStr);
                int cols = Integer.parseInt(colsStr);

                // ایجاد spreadsheet جدید
                Spreadsheet newSpreadsheet = new Spreadsheet(rows, cols);
                // به‌روزرسانی referenceها (نیاز به refactoring دارد)

                tableModel.fireTableDataChanged();
                outputArea.append("New spreadsheet created: " + rows + "x" + cols + "\n");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid dimensions!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showStatistics() {
        SpreadsheetView view = new SpreadsheetView(spreadsheet);

        JTextArea statsArea = new JTextArea(20, 50);
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        statsArea.setText("Grid Statistics:\n");

        JOptionPane.showMessageDialog(this,
                new JScrollPane(statsArea),
                "Spreadsheet Statistics",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "Excel Spreadsheet Simulator\n" +
                        "Version 1.0\n" +
                        "Built with Java Swing\n" +
                        "© 2024 Excel Team",
                "About",
                JOptionPane.INFORMATION_MESSAGE);
    }

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
        """;

        JTextArea helpArea = new JTextArea(helpText);
        helpArea.setEditable(false);
        helpArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JOptionPane.showMessageDialog(this,
                new JScrollPane(helpArea),
                "Help",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void initializeGUI() {
        setTitle("Excel Spreadsheet Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        createMenuBar(); // اضافه شده

        createTable();
        createControlPanel();
        createOutputArea();

        setSize(1200, 800);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
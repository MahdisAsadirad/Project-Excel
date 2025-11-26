package org.example.model;
import org.example.utils.CellConverter;

public class HistoryManager {
    private final Stack<SpreadsheetState> undoStack;
    private final Stack<SpreadsheetState> redoStack;
    private final int maxHistorySize;
    private boolean isRecording;

    public HistoryManager() {
        this(50);
    }

    public HistoryManager(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
        this.undoStack = new Stack<>(maxHistorySize);
        this.redoStack = new Stack<>(maxHistorySize);
        this.isRecording = true;
    }

    public void saveState(Spreadsheet spreadsheet) {
        if (!isRecording) return;

        if (!redoStack.isEmpty()) {
            redoStack.clear();
        }
        SpreadsheetState state = createState(spreadsheet);
        if (undoStack.isFull()) {
            removeOldestState();
        }
        undoStack.push(state);
    }

    private SpreadsheetState createState(Spreadsheet spreadsheet) {
        SpreadsheetState state = new SpreadsheetState(spreadsheet.getRows(), spreadsheet.getCols());

        for (int row = 0; row < spreadsheet.getRows(); row++) {
            for (int col = 0; col < spreadsheet.getCols(); col++) {
                String cellRef = CellConverter.toCellReference(row, col);
                Cell cell = spreadsheet.getCell(cellRef);
                state.saveCellState(cellRef, cell);
            }
        }
        return state;
    }

    private void removeOldestState() {
        Stack<SpreadsheetState> tempStack = new Stack<>(maxHistorySize);
        while (undoStack.size() > 1) {
            tempStack.push(undoStack.pop());
        }
        undoStack.pop();
        while (!tempStack.isEmpty()) {
            undoStack.push(tempStack.pop());
        }
    }

    public boolean undo(Spreadsheet spreadsheet) {
        if (undoStack.isEmpty() || undoStack.size() < 2) {
            return false;
        }

        try {
            isRecording = false;

            SpreadsheetState currentState = createState(spreadsheet);
            redoStack.push(currentState);

            SpreadsheetState previousState = undoStack.pop();
            applyState(spreadsheet, previousState);

            return true;

        } finally {
            isRecording = true;
        }
    }

    public boolean redo(Spreadsheet spreadsheet) {
        if (redoStack.isEmpty()) {
            return false;
        }

        try {
            isRecording = false;

            SpreadsheetState currentState = createState(spreadsheet);
            undoStack.push(currentState);

            SpreadsheetState nextState = redoStack.pop();
            applyState(spreadsheet, nextState);

            return true;

        } finally {
            isRecording = true;
        }
    }

    private void applyState(Spreadsheet spreadsheet, SpreadsheetState state) {
        for (int row = 0; row < spreadsheet.getRows(); row++) {
            for (int col = 0; col < spreadsheet.getCols(); col++) {
                String cellRef = CellConverter.toCellReference(row, col);
                var cellState = state.getCellState(cellRef);

                if (cellState != null) {
                    Cell cell = spreadsheet.getCell(cellRef);
                    cellState.applyToCell(cell);
                }
            }
        }

        spreadsheet.recalculateAll();
    }

    public boolean canUndo() {
        return undoStack.size() >= 2;
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

}

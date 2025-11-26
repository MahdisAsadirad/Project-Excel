package org.example.model;
import org.example.model.Stack;
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

        System.out.println("DEBUG: State saved. Undo stack size: " + undoStack.size());
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
            System.out.println("DEBUG: Cannot undo - insufficient history");
            return false;
        }

        try {
            isRecording = false;

            SpreadsheetState currentState = createState(spreadsheet);
            redoStack.push(currentState);

            SpreadsheetState previousState = undoStack.pop();
            applyState(spreadsheet, previousState);

            System.out.println("DEBUG: Undo performed. Undo stack: " + undoStack.size() +
                    ", Redo stack: " + redoStack.size());
            return true;

        } finally {
            isRecording = true;
        }
    }

    public boolean redo(Spreadsheet spreadsheet) {
        if (redoStack.isEmpty()) {
            System.out.println("DEBUG: Cannot redo - no actions to redo");
            return false;
        }

        try {
            isRecording = false;

            SpreadsheetState currentState = createState(spreadsheet);
            undoStack.push(currentState);

            SpreadsheetState nextState = redoStack.pop();
            applyState(spreadsheet, nextState);

            System.out.println("DEBUG: Redo performed. Undo stack: " + undoStack.size() +
                    ", Redo stack: " + redoStack.size());
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

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        System.out.println("DEBUG: History cleared");
    }

    public void clearRedo() {
        redoStack.clear();
    }

    public boolean canUndo() {
        return undoStack.size() >= 2;
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public int getUndoCount() {
        return Math.max(0, undoStack.size() - 1);
    }

    public int getRedoCount() {
        return redoStack.size();
    }

    public String getHistoryInfo() {
        return String.format("Undo: %d available, Redo: %d available",
                getUndoCount(), getRedoCount());
    }

    public void setRecording(boolean recording) {
        this.isRecording = recording;
    }

    public boolean isRecording() {
        return isRecording;
    }
}

package org.example.model;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class SpreadsheetState implements Serializable {
    private final Map<String, CellState> cellStates;
    private final int rows;
    private final int cols;
    private final long timestamp;

    public SpreadsheetState(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.cellStates = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    public void saveCellState(String cellReference, Cell cell) {
        cellStates.put(cellReference, new CellState(cell));
    }

    public CellState getCellState(String cellReference) {
        return cellStates.get(cellReference);
    }

    public Map<String, CellState> getAllCellStates() {
        return new HashMap<>(cellStates);
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public long getTimestamp() { return timestamp; }

    public boolean isEmpty() {
        return cellStates.isEmpty();
    }

    // Deep copy implementation
    public SpreadsheetState deepCopy() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (SpreadsheetState) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to deep copy SpreadsheetState", e);
        }
    }
}
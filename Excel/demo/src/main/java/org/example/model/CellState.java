package org.example.model;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class CellState implements Serializable {
    private final String rawContent;
    private final Object computedValue;
    private final CellType cellType;
    private final Set<String> dependencies;
    private final ErrorType errorType;
    private final String errorMessage;

    public CellState(Cell cell) {
        this.rawContent = cell.getRawContent();
        this.computedValue = deepCopyValue(cell.getComputedValue());
        this.cellType = cell.getCellType();
        this.dependencies = new HashSet<>(cell.getDependencies());
        this.errorType = cell.getErrorType();
        this.errorMessage = cell.getErrorMessage();
    }

    private Object deepCopyValue(Object value) {
        if (value == null) return null;

        if (value instanceof Number || value instanceof String) {
            return value;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (Exception e) {
            // اگر serialization ممکن نبود، مقدار اصلی را برمی‌گردانیم
            return value;
        }
    }

    // Getter methods
    public String getRawContent() { return rawContent; }
    public Object getComputedValue() { return computedValue; }
    public CellType getCellType() { return cellType; }
    public Set<String> getDependencies() { return new HashSet<>(dependencies); }
    public ErrorType getErrorType() { return errorType; }
    public String getErrorMessage() { return errorMessage; }

    public void applyToCell(Cell cell) {
        cell.setRawContent(this.rawContent);
        cell.setComputedValue(this.computedValue);
        cell.setCellType(this.cellType);
        cell.setDependencies(this.dependencies);
        cell.setErrorType(this.errorType);
        cell.setErrorMessage(this.errorMessage);
    }
}
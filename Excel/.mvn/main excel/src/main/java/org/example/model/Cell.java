package org.example.model;


import org.example.utils.MathHelper;

import java.util.HashSet;
import java.util.Set;

public class Cell {
    private String rawContent;
    private Object computedValue;
    private CellType cellType;
    private Set<String> dependencies; // سلول‌هایی که این سلول به آنها وابسته است
    private ErrorType errorType;
    private String errorMessage;
    private boolean visited; // برای الگوریتم‌های پیمایش

    public Cell() {
        this.rawContent = "";
        this.computedValue = null;
        this.cellType = CellType.EMPTY;
        this.dependencies = new HashSet<>();
        this.errorType = ErrorType.NO_ERROR;
        this.errorMessage = "";
        this.visited = false;
    }

    public Cell(String rawContent, CellType cellType) {
        this();
        this.rawContent = rawContent;
        this.cellType = cellType;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent != null ? rawContent : "";
    }

    public Object getComputedValue() {
        return computedValue;
    }

    public void setComputedValue(Object computedValue) {
        this.computedValue = computedValue;
    }

    public CellType getCellType() {
        return cellType;
    }

    public void setCellType(CellType cellType) {
        this.cellType = cellType;
    }

    public Set<String> getDependencies() {
        return new HashSet<>(dependencies);
    }

    public void setDependencies(Set<String> dependencies) {
        this.dependencies = new HashSet<>(dependencies != null ? dependencies : new HashSet<>());
    }

    public void clearDependencies() {
        this.dependencies.clear();
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType != null ? errorType : ErrorType.NO_ERROR;
        if (this.errorType == ErrorType.NO_ERROR) {
            this.errorMessage = "";
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage != null ? errorMessage : "";
    }

    public boolean hasError() {
        return errorType != ErrorType.NO_ERROR;
    }

    public void clearError() {
        this.errorType = ErrorType.NO_ERROR;
        this.errorMessage = "";
    }

    public boolean isEmpty() {
        return (rawContent == null || rawContent.isEmpty()) &&
                !hasError() &&
                computedValue == null;
    }

    public double getNumericValue() {
        if (hasError()) {
            throw new IllegalStateException("Cell has error: " + errorMessage);
        }

        if (computedValue instanceof Number) {
            return ((Number) computedValue).doubleValue();
        } else if (computedValue instanceof String) {
            try {
                return Double.parseDouble((String) computedValue);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Cell does not contain a numeric value");
            }
        } else {
            throw new IllegalStateException("Cell does not contain a numeric value");
        }
    }

    public String getStringValue() {
        if (hasError()) {
            return "#ERR!";
        }

        if (computedValue != null) {
            return computedValue.toString();
        }

        return "";
    }


    @Override
    public int hashCode() {
        return java.util.Objects.hash(rawContent, computedValue, cellType, errorType, dependencies);
    }

    public String getDisplayValue() {
        if (hasError()) {
            return "#ERR!";
        }

        if (computedValue != null) {
            if (computedValue instanceof Double) {
                return MathHelper.formatNumber((Double) computedValue);
            }
            return computedValue.toString();
        }

        if (rawContent == null || rawContent.isEmpty()) {
            return "";
        }

        if (cellType == CellType.TEXT) {
            if (rawContent.startsWith("\"") && rawContent.endsWith("\"")) {
                return rawContent.substring(1, rawContent.length() - 1);
            }
            return rawContent;
        }

        if (cellType == CellType.NUMBER) {
            return rawContent;
        }

        if (cellType == CellType.FORMULA) {
            return rawContent;
        }

        return "";
    }

}



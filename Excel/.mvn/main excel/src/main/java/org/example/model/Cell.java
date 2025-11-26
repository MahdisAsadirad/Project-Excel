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

    // Getter و Setter methods
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

    public void addDependency(String cellReference) {
        if (cellReference != null && !cellReference.trim().isEmpty()) {
            this.dependencies.add(cellReference.trim().toUpperCase());
        }
    }

    public void removeDependency(String cellReference) {
        this.dependencies.remove(cellReference);
    }

    public void clearDependencies() {
        this.dependencies.clear();
    }

    public boolean hasDependency(String cellReference) {
        return dependencies.contains(cellReference);
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
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

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public boolean hasError() {
        return errorType != ErrorType.NO_ERROR;
    }

    public void clearError() {
        this.errorType = ErrorType.NO_ERROR;
        this.errorMessage = "";
    }

    public void setError(ErrorType errorType, String errorMessage) {
        this.errorType = errorType;
        this.errorMessage = errorMessage != null ? errorMessage : "";
        this.computedValue = null;
    }

    public boolean isEmpty() {
        return (rawContent == null || rawContent.isEmpty()) &&
                !hasError() &&
                computedValue == null;
    }

    public void clear() {
        this.rawContent = "";
        this.computedValue = null;
        this.cellType = CellType.EMPTY;
        this.dependencies.clear();
        this.errorType = ErrorType.NO_ERROR;
        this.errorMessage = "";
        this.visited = false;
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
    public String toString() {
        if (hasError()) {
            return "#ERR!";
        }

        switch (cellType) {
            case EMPTY:
                return "-";
            case TEXT:
                return computedValue != null ? computedValue.toString() :
                        (rawContent.startsWith("\"") && rawContent.endsWith("\"") ?
                                rawContent.substring(1, rawContent.length() - 1) : rawContent);
            case NUMBER:
            case FORMULA:
                if (computedValue instanceof Double) {
                    return MathHelper.formatNumber((Double) computedValue);
                } else if (computedValue instanceof Integer) {
                    return String.valueOf(computedValue);
                } else if (computedValue != null) {
                    return computedValue.toString();
                } else {
                    return "-";
                }
            case ERROR:
                return "#ERR!";
            default:
                return "--";
        }
    }

    public String toDetailedString() {
        return String.format(
                "Cell{rawContent='%s', computedValue=%s, cellType=%s, errorType=%s, dependencies=%s}",
                rawContent, computedValue, cellType, errorType, dependencies
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Cell other = (Cell) obj;
        return java.util.Objects.equals(rawContent, other.rawContent) &&
                java.util.Objects.equals(computedValue, other.computedValue) &&
                cellType == other.cellType &&
                errorType == other.errorType &&
                java.util.Objects.equals(dependencies, other.dependencies);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(rawContent, computedValue, cellType, errorType, dependencies);
    }

    public Cell copy() {
        Cell copy = new Cell();
        copy.rawContent = this.rawContent;
        copy.computedValue = this.computedValue;
        copy.cellType = this.cellType;
        copy.dependencies = new HashSet<>(this.dependencies);
        copy.errorType = this.errorType;
        copy.errorMessage = this.errorMessage;
        copy.visited = this.visited;
        return copy;
    }

    public String getDisplayValue() {
        // اگر خطا دارد، مستقیماً نشان بده
        if (hasError()) {
            return "#ERR!";
        }

        // اگر محاسبه شده وجود دارد → همان را نمایش بده
        if (computedValue != null) {
            if (computedValue instanceof Double) {
                return MathHelper.formatNumber((Double) computedValue);
            }
            return computedValue.toString();
        }

        // rawContent خالی = سلول خالی
        if (rawContent == null || rawContent.isEmpty()) {
            return "";
        }

        // اگر متن است → کوئوت‌ها را حذف کن
        if (cellType == CellType.TEXT) {
            if (rawContent.startsWith("\"") && rawContent.endsWith("\"")) {
                return rawContent.substring(1, rawContent.length() - 1);
            }
            return rawContent;
        }

        // اگر عدد است اما هنوز computedValue ست نشده
        if (cellType == CellType.NUMBER) {
            return rawContent;
        }

        // اگر فرمول هنوز evaluate نشده
        if (cellType == CellType.FORMULA) {
            return rawContent; // مثل Excel که تا محاسبه نشه همون فرمول رو نشون می‌ده
        }

        // حالت‌های نامشخص → خالی
        return "";
    }

}

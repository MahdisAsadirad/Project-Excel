package org.example.model;

import org.example.controller.ExpressionParser;
import org.example.controller.FormulaEvaluator;
import org.example.exceptions.CircularDependencyException;
import org.example.exceptions.InvalidReferenceException;
import org.example.utils.CellReferenceConverter;
import org.example.utils.ValidationUtils;

import java.util.*;

public class Spreadsheet {
    private final CellArray grid;
    private final Map<String, Set<String>> dependencyGraph;
    private final int rows;
    private final int cols;
    private final Set<String> calculationInProgress;
    private final HistoryManager historyManager;

    public Spreadsheet(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new CellArray(rows, cols);
        this.dependencyGraph = new HashMap<>();
        this.calculationInProgress = new HashSet<>();
        this.historyManager = new HistoryManager();
        initializeDependencyGraph();
        historyManager.saveState(this);
    }

    private void initializeDependencyGraph() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                String cellRef = toCellReference(i, j);
                dependencyGraph.put(cellRef, new HashSet<>());
            }
        }
    }

    public void saveState() {
        historyManager.saveState(this);
    }

    public boolean undo() {
        boolean result = historyManager.undo(this);
        if (result) {
            System.out.println("Undo performed successfully");
        }
        return result;
    }

    public boolean redo() {
        boolean result = historyManager.redo(this);
        if (result) {
            System.out.println("Redo performed successfully");
        }
        return result;
    }

    public boolean canUndo() {
        return historyManager.canUndo();
    }

    public boolean canRedo() {
        return historyManager.canRedo();
    }

    public String getHistoryInfo() {
        return historyManager.getHistoryInfo();
    }


    public Cell getCell(int row, int col) {
        return grid.getCell(row, col);
    }

    public Cell getCell(String cellReference) {
        validateCellReference(cellReference);
        return grid.getCell(cellReference);
    }

    public void setCellContent(String cellReference, String content) {
        historyManager.saveState(this);
        validateCellReference(cellReference);
        int[] coordinates = CellReferenceConverter.fromCellReference(cellReference);
        setCellContent(coordinates[0], coordinates[1], content);
    }

    public void setCellContent(int row, int col, String content) {
        historyManager.saveState(this);
        validateCoordinates(row, col);
        Cell cell = grid.getCell(row, col);
        String cellRef = toCellReference(row, col);

        removeDependencies(cellRef);
        cell.clearDependencies();
        cell.clearError();

        if (content == null || content.trim().isEmpty()) {
            cell.setRawContent("");
            cell.setCellType(CellType.EMPTY);
            cell.setComputedValue(null);
            return;
        }

        String trimmedContent = content.trim();
        cell.setRawContent(trimmedContent);

        try {
            if (ValidationUtils.isFormula(trimmedContent)) {
                System.out.println("  -> Processing as FORMULA: " + trimmedContent);
                cell.setCellType(CellType.FORMULA);
                String formula = ValidationUtils.extractFormula(trimmedContent);
                processFormula(cell, formula, cellRef);

            } else if (ValidationUtils.isTextContent(trimmedContent)) {
                System.out.println("  -> Processing as TEXT");
                String textValue = ValidationUtils.extractTextContent(trimmedContent);
                cell.setCellType(CellType.TEXT);
                cell.setComputedValue(textValue);

            } else if (ValidationUtils.isNumberContent(trimmedContent)) {
                System.out.println("  -> Processing as NUMBER");
                double numericValue = Double.parseDouble(trimmedContent);
                cell.setCellType(CellType.NUMBER);
                cell.setComputedValue(numericValue);

            } else {
                System.out.println("  -> Processing as PLAIN TEXT");
                cell.setCellType(CellType.TEXT);
                cell.setComputedValue(trimmedContent);
            }
        } catch (Exception e) {
            System.out.println("  -> ERROR: " + e.getMessage());
            cell.setCellType(CellType.ERROR);
            cell.setErrorType(ErrorType.INVALID_FORMULA);
            cell.setErrorMessage(e.getMessage());
            propagateError(cellRef);
        }
    }

    public void clearHistory() {
        historyManager.clear();
    }

    public void enableHistoryRecording() {
        historyManager.setRecording(true);
    }

    public void disableHistoryRecording() {
        historyManager.setRecording(false);
    }

    private void processFormula(Cell cell, String formula, String currentCellRef) {
        try {
            System.out.println("DEBUG: Processing formula: " + formula + " for cell: " + currentCellRef);

            ValidationUtils.validateFormula(formula);

            // استخراج وابستگی‌ها از فرمول (شامل سلول‌ها در توابع تجمعی)
            Set<String> dependencies = ExpressionParser.extractCellReferences(formula);
            cell.setDependencies(dependencies);

            System.out.println("DEBUG: Dependencies found: " + dependencies);

            // اضافه کردن وابستگی‌ها به گراف
            for (String dependency : dependencies) {
                validateCellReference(dependency);
                if (dependency.equals(currentCellRef)) {
                    throw new CircularDependencyException("Self-reference detected in " + currentCellRef);
                }
                addDependency(dependency, currentCellRef);
            }

            // بررسی وابستگی دورانی
            if (hasCircularDependency(currentCellRef)) {
                throw new CircularDependencyException("Circular dependency detected involving " + currentCellRef);
            }

            // محاسبه مقدار فرمول
            calculateFormulaValue(cell, formula, currentCellRef);

        } catch (Exception e) {
            System.out.println("DEBUG: Formula processing failed: " + e.getMessage());
            cell.setErrorType(ErrorType.INVALID_FORMULA);
            cell.setErrorMessage(e.getMessage());
            propagateError(currentCellRef);
        }
    }

    private void calculateFormulaValue(Cell cell, String formula, String currentCellRef) {
        try {
            FormulaEvaluator evaluator = new FormulaEvaluator(this);
            Object result = evaluator.evaluateFormula(formula, currentCellRef);
            cell.setComputedValue(result);
            cell.clearError();
        } catch (Exception e) {
            cell.setErrorType(ErrorType.INVALID_FORMULA);
            cell.setErrorMessage(e.getMessage());
        }
    }

    private void addDependency(String fromCell, String toCell) {
        dependencyGraph.computeIfAbsent(fromCell, k -> new HashSet<>()).add(toCell);
    }

    private void removeDependencies(String cellReference) {
        for (Set<String> dependents : dependencyGraph.values()) {
            dependents.remove(cellReference);
        }
        dependencyGraph.remove(cellReference);
    }

    public boolean hasCircularDependency(String startCell) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        return checkCircularDependency(startCell, visited, recursionStack);
    }

    private boolean checkCircularDependency(String cell, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(cell)) {
            return true;
        }
        if (visited.contains(cell)) {
            return false;
        }

        visited.add(cell);
        recursionStack.add(cell);

        Set<String> dependents = dependencyGraph.getOrDefault(cell, new HashSet<>());
        for (String dependent : dependents) {
            if (checkCircularDependency(dependent, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(cell);
        return false;
    }

    private void propagateError(String errorCellRef) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new Queue<>();
        queue.enqueue(errorCellRef);

        while (!queue.isEmpty()) {
            String currentRef = queue.dequeue();
            if (visited.contains(currentRef)) {
                continue;
            }
            visited.add(currentRef);

            Cell currentCell = getCell(currentRef);
            if (!currentCell.hasError()) {
                currentCell.setErrorType(ErrorType.VALUE_ERROR);
                currentCell.setErrorMessage("Dependent on erroneous cell");
            }

            Set<String> dependents = dependencyGraph.getOrDefault(currentRef, new HashSet<>());
            for (String dependent : dependents) {
                if (!visited.contains(dependent)) {
                    queue.enqueue(dependent);
                }
            }
        }
    }

    public Set<String> getDependents(String cellReference) {
        validateCellReference(cellReference);
        return new HashSet<>(dependencyGraph.getOrDefault(cellReference, new HashSet<>()));
    }

    public Set<String> getDependencies(String cellReference) {
        validateCellReference(cellReference);
        Cell cell = getCell(cellReference);
        return cell.getDependencies();
    }

    public void recalculateAll() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Cell cell = grid.getCell(i, j);
                if (cell.getCellType() == CellType.FORMULA) {
                    String cellRef = toCellReference(i, j);
                    try {
                        String formula = ValidationUtils.extractFormula(cell.getRawContent());
                        calculateFormulaValue(cell, formula, cellRef);
                    } catch (Exception e) {
                        cell.setErrorType(ErrorType.INVALID_FORMULA);
                        cell.setErrorMessage(e.getMessage());
                    }
                }
            }
        }
    }

    public boolean isValidCellReference(String cellReference) {
        return grid.isValidCellReference(cellReference);
    }

    public boolean isValidCoordinate(int row, int col) {
        return grid.isValidCoordinate(row, col);
    }

    private void validateCellReference(String cellReference) {
        if (!isValidCellReference(cellReference)) {
            throw new InvalidReferenceException(cellReference);
        }
    }

    private void validateCoordinates(int row, int col) {
        if (!isValidCoordinate(row, col)) {
            throw new IndexOutOfBoundsException(
                    "Invalid coordinates: (" + row + ", " + col + "). " +
                            "Grid size: " + rows + "x" + cols
            );
        }
    }

    public static String toCellReference(int row, int col) {
        return CellReferenceConverter.toCellReference(row, col);
    }

    public static int[] fromCellReference(String cellReference) {
        return CellReferenceConverter.fromCellReference(cellReference);
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public void clear() {
        grid.clear();
        dependencyGraph.clear();
        initializeDependencyGraph();
    }

    public void displayGrid() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.print(grid.getCell(i, j).toString());
                if (j < cols - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println();
        }
    }

    public String getGridAsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                sb.append(grid.getCell(i, j).toString());
                if (j < cols - 1) {
                    sb.append(", ");
                }
            }
            if (i < rows - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public void debugCellContent(String cellReference) {
        Cell cell = getCell(cellReference);
        System.out.println("DEBUG " + cellReference + ":");
        System.out.println("  Raw: '" + cell.getRawContent() + "'");
        System.out.println("  Type: " + cell.getCellType());
        System.out.println("  IsFormula: " + ValidationUtils.isFormula(cell.getRawContent()));
        System.out.println("  Computed: " + cell.getComputedValue());
        System.out.println("  HasError: " + cell.hasError());
        if (cell.hasError()) {
            System.out.println("  Error: " + cell.getErrorMessage());
        }
    }

    public void cleanup() {
        dependencyGraph.clear();
        calculationInProgress.clear();
    }
}

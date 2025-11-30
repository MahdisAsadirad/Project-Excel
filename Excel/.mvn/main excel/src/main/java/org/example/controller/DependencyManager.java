package org.example.controller;

import org.example.exceptions.CircularDependencyException;
import org.example.model.Cell;
import org.example.model.CellType;
import org.example.model.ErrorType;
import org.example.model.Spreadsheet;
import org.example.model.Queue;

import java.util.*;

public class DependencyManager {
    private final Spreadsheet spreadsheet;
    private final FormulaEvaluator formulaEvaluator;

    public DependencyManager(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
        this.formulaEvaluator = new FormulaEvaluator(spreadsheet);
    }

    public void recalculateDependencies(String changedCell) {
        Set<String> affectedCells = findAffectedCells(changedCell);
        List<String> topologicalOrder = getTopologicalOrder(affectedCells);

        for (String cellRef : topologicalOrder) {
            recalculateCell(cellRef);
        }
    }

    public Set<String> findAffectedCells(String startCell) {
        Set<String> affected = new HashSet<>();
        Queue<String> queue = new Queue<>();
        queue.enqueue(startCell);

        while (!queue.isEmpty()) {
            String current = queue.dequeue();
            affected.add(current);

            Set<String> dependents = spreadsheet.getDependents(current);
            for (String dependent : dependents) {
                if (!affected.contains(dependent)) {
                    queue.enqueue(dependent);
                }
            }
        }

        return affected;
    }

    private List<String> getTopologicalOrder(Set<String> cells) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Set<String>> graph = new HashMap<>();

        for (String cell : cells) {
            inDegree.put(cell, 0);
            graph.put(cell, new HashSet<>());
        }

        for (String cell : cells) {
            Set<String> dependencies = spreadsheet.getDependencies(cell);
            for (String dependency : dependencies) {
                if (cells.contains(dependency)) {
                    graph.get(dependency).add(cell);
                    inDegree.put(cell, inDegree.get(cell) + 1);
                }
            }
        }

        Queue<String> zeroInDegreeQueue = new Queue<>();
        for (String cell : cells) {
            if (inDegree.get(cell) == 0) {
                zeroInDegreeQueue.enqueue(cell);
            }
        }

        List<String> topologicalOrder = new ArrayList<>();
        while (!zeroInDegreeQueue.isEmpty()) {
            String current = zeroInDegreeQueue.dequeue();
            topologicalOrder.add(current);

            for (String neighbor : graph.get(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    zeroInDegreeQueue.enqueue(neighbor);
                }
            }
        }

        if (topologicalOrder.size() != cells.size()) {
            throw new CircularDependencyException(
                    "Circular dependency detected in affected cells set"
            );
        }

        return topologicalOrder;
    }

    private void recalculateCell(String cellReference) {
        Cell cell = spreadsheet.getCell(cellReference);

        if (cell.getCellType() != CellType.FORMULA) {
            return;
        }

        try {
            String formula = cell.getRawContent().substring(1); // حذف '='
            formulaEvaluator.updateCellFormula(cell, formula, cellReference);

            cell.clearError();

        } catch (Exception e) {
            cell.setErrorType(ErrorType.VALUE_ERROR);
            cell.setErrorMessage(e.getMessage());
            propagateError(cellReference);
        }
    }


    private void propagateError(String errorCellRef) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new Queue<>();
        queue.enqueue(errorCellRef);

        while (!queue.isEmpty()) {
            String current = queue.dequeue();
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            Cell currentCell = spreadsheet.getCell(current);
            if (current.equals(errorCellRef)) {
                continue;
            }

            if (!currentCell.hasError()) {
                currentCell.setErrorType(ErrorType.VALUE_ERROR);
                currentCell.setErrorMessage("Dependent on erroneous cell: " + errorCellRef);
            }

            Set<String> dependents = spreadsheet.getDependents(current);
            for (String dependent : dependents) {
                if (!visited.contains(dependent)) {
                    queue.enqueue(dependent);
                }
            }
        }
    }
}


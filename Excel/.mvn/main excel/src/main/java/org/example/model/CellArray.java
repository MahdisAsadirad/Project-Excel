package org.example.model;

import org.example.utils.CellConverter;

public class CellArray {
    private final Cell[][] grid;
    private final int rows;
    private final int cols;

    public CellArray(int rows, int cols) {
        if (rows <= 0 || cols <= 0 || rows > 26 || cols > 26) {
            throw new IllegalArgumentException("Invalid grid dimensions: " + rows + "x" + cols);
        }
        this.rows = rows;
        this.cols = cols;
        this.grid = new Cell[rows][cols];
        initializeGrid();
    }

    private void initializeGrid() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = new Cell();
            }
        }
    }

    public Cell getCell(int row, int col) {
        validateCoordinates(row, col);
        return grid[row][col];
    }

    public Cell getCell(String cellReference) {
        int[] coordinates = CellConverter.fromCellReference(cellReference);
        return getCell(coordinates[0], coordinates[1]);
    }

    public boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public boolean isValidCellReference(String cellReference) {
        try {
            int[] coordinates = CellConverter.fromCellReference(cellReference);
            return isValidCoordinate(coordinates[0], coordinates[1]);
        } catch (Exception e) {
            return false;
        }
    }

    private void validateCoordinates(int row, int col) {
        if (!isValidCoordinate(row, col)) {
            throw new IndexOutOfBoundsException(
                    "Invalid cell coordinates: (" + row + ", " + col + "). " +
                            "Grid size: " + rows + "x" + cols
            );
        }
    }

    public void clear() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = new Cell();
            }
        }
    }
}

package org.example.excel.controller;

import org.example.excel.model.Cell;
import org.example.excel.model.CellType;
import org.example.excel.model.Spreadsheet;
import org.example.excel.utils.CellReferenceConverter;

import java.util.regex.Pattern;

public class AutoFillManager {
    private final Spreadsheet spreadsheet;
    private final Pattern cellRefPattern = Pattern.compile("[A-Za-z]\\d+");

    public AutoFillManager(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
    }

    public void autoFill(String sourceCell, String targetRange) {
        validateParameters(sourceCell, targetRange);

        int[] sourceCoords = CellReferenceConverter.fromCellReference(sourceCell);
        Cell source = spreadsheet.getCell(sourceCoords[0], sourceCoords[1]);

        String[] rangeParts = targetRange.split(":");
        if (rangeParts.length != 2) {
            throw new IllegalArgumentException("Invalid range format: " + targetRange);
        }

        int[] startCoords = CellReferenceConverter.fromCellReference(rangeParts[0]);
        int[] endCoords = CellReferenceConverter.fromCellReference(rangeParts[1]);

        validateRange(startCoords, endCoords);

        performAutoFill(source, sourceCoords, startCoords, endCoords);
    }

    private void validateParameters(String sourceCell, String targetRange) {
        if (!spreadsheet.isValidCellReference(sourceCell)) {
            throw new IllegalArgumentException("Invalid source cell: " + sourceCell);
        }

        if (!targetRange.contains(":")) {
            throw new IllegalArgumentException("Invalid range format: " + targetRange);
        }

        String[] rangeParts = targetRange.split(":");
        if (rangeParts.length != 2) {
            throw new IllegalArgumentException("Invalid range format: " + targetRange);
        }

        if (!spreadsheet.isValidCellReference(rangeParts[0]) ||
                !spreadsheet.isValidCellReference(rangeParts[1])) {
            throw new IllegalArgumentException("Invalid cell in range: " + targetRange);
        }
    }

    private void validateRange(int[] start, int[] end) {
        if (start[0] > end[0] || start[1] > end[1]) {
            throw new IllegalArgumentException("Invalid range: start must be before end");
        }
    }

    private void performAutoFill(Cell source, int[] sourceCoords, int[] startCoords, int[] endCoords) {
        String sourceContent = source.getRawContent();
        CellType sourceType = source.getCellType();

        for (int row = startCoords[0]; row <= endCoords[0]; row++) {
            for (int col = startCoords[1]; col <= endCoords[1]; col++) {
                if (row == sourceCoords[0] && col == sourceCoords[1]) {
                    continue;
                }

                String content = adjustContentForPosition(
                        sourceContent, sourceType, sourceCoords, new int[]{row, col}
                );

                String cellRef = CellReferenceConverter.toCellReference(row, col);
                spreadsheet.setCellContent(cellRef, content);
            }
        }
    }

    private String adjustContentForPosition(String sourceContent, CellType sourceType,
                                            int[] sourceCoords, int[] targetCoords) {
        if (sourceType != CellType.FORMULA) {
            return sourceContent;
        }

        return adjustFormulaReferences(sourceContent, sourceCoords, targetCoords);
    }

    private String adjustFormulaReferences(String formula, int[] sourceCoords, int[] targetCoords) {
        StringBuilder adjustedFormula = new StringBuilder();
        StringBuilder currentRef = new StringBuilder();
        boolean inReference = false;

        for (char c : formula.toCharArray()) {
            if (Character.isLetter(c)) {
                inReference = true;
                currentRef.append(c);
            } else if (Character.isDigit(c) && inReference) {
                currentRef.append(c);
            } else {
                if (inReference && currentRef.length() > 0) {
                    String ref = currentRef.toString();
                    if (cellRefPattern.matcher(ref).matches()) {
                        String adjustedRef = adjustCellReference(ref, sourceCoords, targetCoords);
                        adjustedFormula.append(adjustedRef);
                    } else {
                        adjustedFormula.append(ref);
                    }
                    currentRef.setLength(0);
                }
                adjustedFormula.append(c);
                inReference = false;
            }
        }

        if (inReference && currentRef.length() > 0) {
            String ref = currentRef.toString();
            if (cellRefPattern.matcher(ref).matches()) {
                String adjustedRef = adjustCellReference(ref, sourceCoords, targetCoords);
                adjustedFormula.append(adjustedRef);
            } else {
                adjustedFormula.append(ref);
            }
        }

        return adjustedFormula.toString();
    }

    private String adjustCellReference(String cellRef, int[] sourceCoords, int[] targetCoords) {
        int[] refCoords = CellReferenceConverter.fromCellReference(cellRef);

        int rowOffset = refCoords[0] - sourceCoords[0];
        int colOffset = refCoords[1] - sourceCoords[1];

        int newRow = targetCoords[0] + rowOffset;
        int newCol = targetCoords[1] + colOffset;

        if (newRow >= 0 && newRow < spreadsheet.getRows() &&
                newCol >= 0 && newCol < spreadsheet.getCols()) {
            return CellReferenceConverter.toCellReference(newRow, newCol);
        } else {
            return cellRef;
        }
    }
}

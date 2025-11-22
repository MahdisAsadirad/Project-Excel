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

        // اجرای AutoFill
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
                // رد کردن سلول منبع
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
            return sourceContent; // برای مقادیر ثابت و متن، بدون تغییر
        }

        // برای فرمول‌ها، ارجاع‌های سلولی را تنظیم می‌کنیم
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

        // بررسی آخرین reference
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

        // محاسبه offset نسبی
        int rowOffset = refCoords[0] - sourceCoords[0];
        int colOffset = refCoords[1] - sourceCoords[1];

        // اعمال offset به موقعیت هدف
        int newRow = targetCoords[0] + rowOffset;
        int newCol = targetCoords[1] + colOffset;

        // بررسی معتبر بودن سلول جدید
        if (newRow >= 0 && newRow < spreadsheet.getRows() &&
                newCol >= 0 && newCol < spreadsheet.getCols()) {
            return CellReferenceConverter.toCellReference(newRow, newCol);
        } else {
            // اگر سلول خارج از محدوده باشد، ارجاع اصلی را نگه می‌داریم
            return cellRef;
        }
    }

    public void fillSeries(String startCell, String endCell, double startValue, double step) {
        int[] startCoords = CellReferenceConverter.fromCellReference(startCell);
        int[] endCoords = CellReferenceConverter.fromCellReference(endCell);

        validateRange(startCoords, endCoords);

        double currentValue = startValue;
        int cellCount = countCellsInRange(startCoords, endCoords);

        for (int i = 0; i < cellCount; i++) {
            int[] cellCoords = getCellAtOffset(startCoords, endCoords, i);
            String cellRef = CellReferenceConverter.toCellReference(cellCoords[0], cellCoords[1]);
            spreadsheet.setCellContent(cellRef, String.valueOf(currentValue));
            currentValue += step;
        }
    }

    private int countCellsInRange(int[] start, int[] end) {
        int rowCount = end[0] - start[0] + 1;
        int colCount = end[1] - start[1] + 1;
        return rowCount * colCount;
    }

    private int[] getCellAtOffset(int[] start, int[] end, int offset) {
        int rowSpan = end[0] - start[0] + 1;
        int colSpan = end[1] - start[1] + 1;

        int rowOffset = offset / colSpan;
        int colOffset = offset % colSpan;

        return new int[]{start[0] + rowOffset, start[1] + colOffset};
    }
}

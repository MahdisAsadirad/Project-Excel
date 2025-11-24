// AggregateFunctions.java
package org.example.excel.utils;

import org.example.excel.model.Spreadsheet;
import org.example.excel.model.Cell;
import org.example.excel.exceptions.InvalidReferenceException;
import org.example.excel.utils.CellReferenceConverter;

import java.util.ArrayList;
import java.util.List;

public class AggregateFunctions {

    // متد اصلی برای تجزیه محدوده
    public static List<Cell> parseRange(Spreadsheet spreadsheet, String range) {
        List<Cell> cells = new ArrayList<>();

        if (range == null || range.trim().isEmpty()) {
            throw new IllegalArgumentException("Range cannot be empty");
        }

        String[] parts = range.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid range format. Use: A1:B5");
        }

        String startRef = parts[0].trim().toUpperCase();
        String endRef = parts[1].trim().toUpperCase();

        if (!spreadsheet.isValidCellReference(startRef) || !spreadsheet.isValidCellReference(endRef)) {
            throw new InvalidReferenceException("Invalid cell reference in range: " + range);
        }

        int[] startCoords = CellReferenceConverter.fromCellReference(startRef);
        int[] endCoords = CellReferenceConverter.fromCellReference(endRef);

        int startRow = Math.min(startCoords[0], endCoords[0]);
        int endRow = Math.max(startCoords[0], endCoords[0]);
        int startCol = Math.min(startCoords[1], endCoords[1]);
        int endCol = Math.max(startCoords[1], endCoords[1]);

        // جمع‌آوری سلول‌های محدوده
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                Cell cell = spreadsheet.getCell(row, col);
                cells.add(cell);
            }
        }

        return cells;
    }

    // محاسبه مجموع
    public static double sum(Spreadsheet spreadsheet, String range) {
        List<Cell> cells = parseRange(spreadsheet, range);
        double total = 0;
        int validCount = 0;

        for (Cell cell : cells) {
            if (cell.hasError()) {
                throw new InvalidReferenceException("Cell in range has error: " +
                        CellReferenceConverter.toCellReference(
                                getRowFromCell(cell), getColFromCell(cell)
                        ));
            }

            try {
                double value = cell.getNumericValue();
                total += value;
                validCount++;
            } catch (IllegalStateException e) {
                // سلول‌هایی که مقدار عددی ندارند نادیده گرفته می‌شوند
                continue;
            }
        }

        if (validCount == 0) {
            throw new IllegalArgumentException("No numeric values found in range: " + range);
        }

        return total;
    }

    // محاسبه میانگین
    public static double average(Spreadsheet spreadsheet, String range) {
        List<Cell> cells = parseRange(spreadsheet, range);
        double total = 0;
        int validCount = 0;

        for (Cell cell : cells) {
            if (cell.hasError()) {
                throw new InvalidReferenceException("Cell in range has error");
            }

            try {
                double value = cell.getNumericValue();
                total += value;
                validCount++;
            } catch (IllegalStateException e) {
                // سلول‌هایی که مقدار عددی ندارند نادیده گرفته می‌شوند
                continue;
            }
        }

        if (validCount == 0) {
            throw new IllegalArgumentException("No numeric values found in range: " + range);
        }

        return total / validCount;
    }

    // پیدا کردن بیشترین مقدار
    public static double max(Spreadsheet spreadsheet, String range) {
        List<Cell> cells = parseRange(spreadsheet, range);
        Double maxValue = null;

        for (Cell cell : cells) {
            if (cell.hasError()) {
                throw new InvalidReferenceException("Cell in range has error");
            }

            try {
                double value = cell.getNumericValue();
                if (maxValue == null || value > maxValue) {
                    maxValue = value;
                }
            } catch (IllegalStateException e) {
                continue;
            }
        }

        if (maxValue == null) {
            throw new IllegalArgumentException("No numeric values found in range: " + range);
        }

        return maxValue;
    }

    // پیدا کردن کمترین مقدار
    public static double min(Spreadsheet spreadsheet, String range) {
        List<Cell> cells = parseRange(spreadsheet, range);
        Double minValue = null;

        for (Cell cell : cells) {
            if (cell.hasError()) {
                throw new InvalidReferenceException("Cell in range has error");
            }

            try {
                double value = cell.getNumericValue();
                if (minValue == null || value < minValue) {
                    minValue = value;
                }
            } catch (IllegalStateException e) {
                continue;
            }
        }

        if (minValue == null) {
            throw new IllegalArgumentException("No numeric values found in range: " + range);
        }

        return minValue;
    }

    // شمارش سلول‌های عددی
    public static int count(Spreadsheet spreadsheet, String range) {
        List<Cell> cells = parseRange(spreadsheet, range);
        int count = 0;

        for (Cell cell : cells) {
            if (cell.hasError()) {
                continue;
            }

            try {
                cell.getNumericValue();
                count++;
            } catch (IllegalStateException e) {
                continue;
            }
        }

        return count;
    }

    // متدهای کمکی برای پیدا کردن موقعیت سلول
    private static int getRowFromCell(Cell cell) {
        // این متد نیاز به پیاده‌سازی دارد - می‌توانید موقعیت سلول را در اسپردشیت ذخیره کنید
        return 0; // placeholder
    }

    private static int getColFromCell(Cell cell) {
        // این متد نیاز به پیاده‌سازی دارد
        return 0; // placeholder
    }
}
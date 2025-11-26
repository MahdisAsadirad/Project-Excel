package org.example.utils;

public class CellConverter {

    public static String toCellReference(int row, int col) {
        validateCoordinates(row, col);
        char columnChar = (char) ('A' + col);
        return "" + columnChar + (row + 1);
    }

    private static void validateCoordinates(int row, int col) {
        if (row < 0 || row >= 26 || col < 0 || col >= 26) {
            throw new IllegalArgumentException("Coordinates out of range: (" + row + ", " + col + ")");
        }
    }

    public static int[] fromCellReference(String cellReference) {
        if (cellReference == null || cellReference.length() < 2) {
            throw new IllegalArgumentException("Invalid cell reference: " + cellReference);
        }

        String ref = cellReference.toUpperCase().trim();
        StringBuilder columnPart = new StringBuilder();
        StringBuilder rowPart = new StringBuilder();

        // جدا کردن بخش ستون و سطر
        int i = 0;
        while (i < ref.length() && Character.isLetter(ref.charAt(i))) {
            columnPart.append(ref.charAt(i));
            i++;
        }

        while (i < ref.length() && Character.isDigit(ref.charAt(i))) {
            rowPart.append(ref.charAt(i));
            i++;
        }

        if (columnPart.isEmpty() || rowPart.isEmpty()) {
            throw new IllegalArgumentException("Invalid cell reference format: " + cellReference);
        }

        // تبدیل ستون به عدد
        int col = 0;
        String columnStr = columnPart.toString();
        for (int j = 0; j < columnStr.length(); j++) {
            col = col * 26 + (columnStr.charAt(j) - 'A' + 1);
        }
        col--;

        int row = Integer.parseInt(rowPart.toString()) - 1;

        validateCoordinates(row, col);

        return new int[]{row, col};
    }

    public static String getColumnName(int col) {
        validateColumn(col);
        return String.valueOf((char) ('A' + col));
    }


    private static void validateColumn(int col) {
        if (col < 0 || col >= 26) {
            throw new IllegalArgumentException("Column index out of range: " + col);
        }
    }
}

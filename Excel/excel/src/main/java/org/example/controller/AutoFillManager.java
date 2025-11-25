package org.example.controller;

import org.example.model.Cell;
import org.example.model.CellType;
import org.example.model.Spreadsheet;
import org.example.utils.CellReferenceConverter;

import java.util.regex.Pattern;

public class AutoFillManager {
    private final Spreadsheet spreadsheet;
    private final Pattern cellRefPattern = Pattern.compile("[A-Za-z]\\d+");
    private final DependencyManager dependencyManager;

    public AutoFillManager(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
        this.dependencyManager = new DependencyManager(spreadsheet);
    }

    public void autoFill(String command) {
        try {
            String[] fillParams = parseFillCommand(command);
            String sourceCell = fillParams[0];
            String targetRange = fillParams[1];

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

            // محاسبه مجدد وابستگی‌ها
            dependencyManager.recalculateDependencies(sourceCell);

        } catch (Exception e) {
            throw new RuntimeException("AutoFill failed: " + e.getMessage(), e);
        }
    }

    private String[] parseFillCommand(String command) {
        // حذف فضاهای اضافه و تبدیل به uppercase
        String cleanCommand = command.toUpperCase().replace("FILL", "").trim();

        // حذف پرانتزها
        if (cleanCommand.startsWith("(") && cleanCommand.endsWith(")")) {
            cleanCommand = cleanCommand.substring(1, cleanCommand.length() - 1);
        }

        // جدا کردن پارامترها
        String[] parts = cleanCommand.split("\\s*,\\s*");
        if (parts.length != 2) {
            throw new IllegalArgumentException("FILL command requires exactly 2 parameters: source and range");
        }

        String sourceCell = parts[0].trim();
        String targetRange = parts[1].trim();

        // اعتبارسنجی فرمت
        if (!isValidCellReference(sourceCell) || !isValidRange(targetRange)) {
            throw new IllegalArgumentException("Invalid cell reference or range format");
        }

        return new String[]{sourceCell, targetRange};
    }

    private boolean isValidCellReference(String ref) {
        return ref.matches("[A-Za-z]\\d+");
    }

    private boolean isValidRange(String range) {
        return range.matches("[A-Za-z]\\d+:[A-Za-z]\\d+");
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

                String adjustedContent = adjustContentForPosition(
                        sourceContent, sourceType, sourceCoords, new int[]{row, col}
                );

                String cellRef = CellReferenceConverter.toCellReference(row, col);

                try {
                    spreadsheet.setCellContent(cellRef, adjustedContent);
                } catch (Exception e) {
                    // در صورت خطا، سلول را با خطا پر کنید
                    Cell targetCell = spreadsheet.getCell(cellRef);
                    targetCell.setRawContent(adjustedContent);
                    targetCell.setCellType(CellType.ERROR);
                    targetCell.setErrorType(org.example.model.ErrorType.VALUE_ERROR);
                    targetCell.setErrorMessage("AutoFill error: " + e.getMessage());
                }
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

        // محاسبه افست نسبی
        int rowOffset = refCoords[0] - sourceCoords[0];
        int colOffset = refCoords[1] - sourceCoords[1];

        // اعمال افست به موقعیت هدف
        int newRow = targetCoords[0] + rowOffset;
        int newCol = targetCoords[1] + colOffset;

        // بررسی معتبر بودن سلول جدید
        if (newRow >= 0 && newRow < spreadsheet.getRows() &&
                newCol >= 0 && newCol < spreadsheet.getCols()) {
            return CellReferenceConverter.toCellReference(newRow, newCol);
        } else {
            // اگر سلول خارج از محدوده است، ارجاع اصلی را نگه دارید
            return cellRef;
        }
    }
}
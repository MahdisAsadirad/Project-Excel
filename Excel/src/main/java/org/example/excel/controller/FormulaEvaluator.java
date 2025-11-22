package org.example.excel.controller;


import org.example.excel.exceptions.InvalidFormulaException;
import org.example.excel.exceptions.InvalidReferenceException;
import org.example.excel.model.Cell;
import org.example.excel.model.Spreadsheet;
import org.example.excel.model.Stack;
import org.example.excel.utils.MathHelper;

import java.util.List;

public class FormulaEvaluator {
    private final Spreadsheet spreadsheet;

    public FormulaEvaluator(Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
    }

    public Object evaluateFormula(String formula, String currentCell) {
        try {
            // حذف فضاهای اضافه
            formula = formula.trim();

            // ارزیابی ساده برای شروع
            if (formula.contains("+")) {
                return evaluateAddition(formula, currentCell);
            } else if (formula.contains("*")) {
                return evaluateMultiplication(formula, currentCell);
            } else if (formula.contains("/")) {
                return evaluateDivision(formula, currentCell);
            } else if (formula.contains("-") && !formula.startsWith("-")) {
                return evaluateSubtraction(formula, currentCell);
            } else {
                // اگر هیچ عملگری نیست، ممکن است یک سلول یا عدد باشد
                return evaluateSingleValue(formula, currentCell);
            }
        } catch (Exception e) {
            throw new InvalidFormulaException("Error evaluating formula: " + formula, e.getMessage());
        }
    }

    private double evaluateAddition(String formula, String currentCell) {
        String[] parts = formula.split("\\+");
        double result = 0;
        for (String part : parts) {
            result += evaluateSingleValue(part.trim(), currentCell);
        }
        return result;
    }

    private double evaluateMultiplication(String formula, String currentCell) {
        String[] parts = formula.split("\\*");
        double result = 1;
        for (String part : parts) {
            result *= evaluateSingleValue(part.trim(), currentCell);
        }
        return result;
    }

    private double evaluateDivision(String formula, String currentCell) {
        String[] parts = formula.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid division format: " + formula);
        }
        double numerator = evaluateSingleValue(parts[0].trim(), currentCell);
        double denominator = evaluateSingleValue(parts[1].trim(), currentCell);

        if (denominator == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return numerator / denominator;
    }

    private double evaluateSubtraction(String formula, String currentCell) {
        String[] parts = formula.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid subtraction format: " + formula);
        }
        double first = evaluateSingleValue(parts[0].trim(), currentCell);
        double second = evaluateSingleValue(parts[1].trim(), currentCell);
        return first - second;
    }

    private double evaluateSingleValue(String value, String currentCell) {
        // بررسی اگر عدد است
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // عدد نیست، ممکن است ثابت یا ارجاع سلول باشد
        }

        // بررسی ثابت‌ها
        if ("PI".equalsIgnoreCase(value)) {
            return Math.PI;
        }
        if ("E".equalsIgnoreCase(value)) {
            return Math.E;
        }

        // بررسی ارجاع سلول
        if (isCellReference(value)) {
            // بررسی self-reference
            if (value.equalsIgnoreCase(currentCell)) {
                throw new IllegalArgumentException("Self-reference detected: " + value);
            }

            Cell cell = spreadsheet.getCell(value);
            if (cell.hasError()) {
                throw new IllegalArgumentException("Referenced cell has error: " + value);
            }

            try {
                return cell.getNumericValue();
            } catch (Exception e) {
                throw new IllegalArgumentException("Referenced cell does not contain numeric value: " + value);
            }
        }

        throw new IllegalArgumentException("Cannot evaluate: " + value);
    }

    private boolean isCellReference(String value) {
        return value.matches("[A-Za-z]\\d+");
    }

    private Object evaluatePostfix(List<String> postfixTokens, String currentCell) {
        Stack<Double> valueStack = new Stack<>();

        for (String token : postfixTokens) {
            if (MathHelper.isNumber(token)) {
                valueStack.push(MathHelper.parseNumber(token));
            } else if (MathHelper.isConstant(token)) {
                valueStack.push(MathHelper.getConstantValue(token));
            } else if (isCellReference(token)) {
                double cellValue = getCellValue(token, currentCell);
                valueStack.push(cellValue);
            } else {
                // عملگر
                if (valueStack.size() < 2) {
                    throw new InvalidFormulaException("Insufficient operands for operator: " + token);
                }

                double b = valueStack.pop();
                double a = valueStack.pop();
                double result = MathHelper.applyOperation(token.charAt(0), a, b);
                valueStack.push(result);
            }
        }

        if (valueStack.size() != 1) {
            throw new InvalidFormulaException("Invalid expression evaluation");
        }

        return valueStack.pop();
    }

    private double getCellValue(String cellReference, String currentCell) {
        // نرمال‌سازی ارجاع سلول
        String normalizedRef = normalizeCellReference(cellReference);

        // بررسی self-reference
        if (normalizedRef.equals(currentCell)) {
            throw new InvalidFormulaException("Self-reference detected: " + cellReference);
        }

        // بررسی وجود سلول
        if (!spreadsheet.getCell(normalizedRef).hasError()) {
            try {
                return spreadsheet.getCell(normalizedRef).getNumericValue();
            } catch (IllegalStateException e) {
                throw new InvalidReferenceException(
                        "Cell " + normalizedRef + " does not contain numeric value"
                );
            }
        } else {
            throw new InvalidReferenceException(
                    "Cell " + normalizedRef + " has error: " +
                            spreadsheet.getCell(normalizedRef).getErrorMessage()
            );
        }
    }

    private String normalizeCellReference(String cellReference) {
        // حذف @ اگر وجود دارد (برای سازگاری با فرمت‌های مختلف)
        String ref = cellReference.replace("@", "");
        return ref.toUpperCase();
    }

    public void updateCellFormula(Cell cell, String formula, String currentCellRef) {
        try {
            Object result = evaluateFormula(formula, currentCellRef);
            cell.setComputedValue(result);
            cell.clearError();
        } catch (Exception e) {
            cell.setComputedValue(null);
            throw e; // خطا به caller منتقل می‌شود
        }
    }
}
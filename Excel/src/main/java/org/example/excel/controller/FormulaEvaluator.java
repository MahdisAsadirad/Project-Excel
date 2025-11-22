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
            // تبدیل به postfix
            List<String> postfixTokens = ExpressionParser.infixToPostfix(formula);

            // ارزیابی postfix
            return evaluatePostfix(postfixTokens, currentCell);
        } catch (Exception e) {
            throw new InvalidFormulaException(formula, e.getMessage());
        }
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

    private boolean isCellReference(String token) {
        return token.matches("[A-Za-z]@?\\d+");
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
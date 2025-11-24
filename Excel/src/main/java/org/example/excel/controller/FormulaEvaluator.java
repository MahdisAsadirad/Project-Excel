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
            // استفاده از ExpressionParser برای تبدیل و ارزیابی
            List<String> postfixTokens = ExpressionParser.infixToPostfix(formula);
            return evaluatePostfix(postfixTokens, currentCell);
        } catch (Exception e) {
            throw new InvalidFormulaException("Error evaluating formula: " + formula, e.getMessage());
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
            } else if (token.startsWith("u")) {
                // عملگر unary (مثل u+ یا u-)
                if (valueStack.isEmpty()) {
                    throw new InvalidFormulaException("Insufficient operands for unary operator: " + token);
                }
                double operand = valueStack.pop();
                double result = applyUnaryOperator(token, operand);
                valueStack.push(result);
            } else {
                // عملگر باینری
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

    private double applyUnaryOperator(String operator, double operand) {
        char op = operator.charAt(1); // حرف دوم (مثلاً + یا -)
        switch (op) {
            case '+':
                return +operand;
            case '-':
                return -operand;
            default:
                throw new InvalidFormulaException("Unknown unary operator: " + operator);
        }
    }

    private double getCellValue(String cellReference, String currentCell) {
        String normalizedRef = normalizeCellReference(cellReference);

        // بررسی self-reference
        if (normalizedRef.equals(currentCell)) {
            throw new InvalidFormulaException("Self-reference detected: " + cellReference);
        }

        Cell cell = spreadsheet.getCell(normalizedRef);
        if (cell.hasError()) {
            throw new InvalidReferenceException(
                    "Cell " + normalizedRef + " has error: " + cell.getErrorMessage()
            );
        }

        try {
            return cell.getNumericValue();
        } catch (IllegalStateException e) {
            throw new InvalidReferenceException(
                    "Cell " + normalizedRef + " does not contain numeric value"
            );
        }
    }

    private boolean isCellReference(String value) {
        return value.matches("[A-Za-z]\\d+");
    }

    private String normalizeCellReference(String cellReference) {
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
            throw e;
        }
    }
}
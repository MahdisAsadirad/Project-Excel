// FormulaEvaluator.java
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
            System.out.println("DEBUG: Evaluating formula: " + formula + " for cell: " + currentCell);

            List<String> postfixTokens = ExpressionParser.infixToPostfix(formula);
            Object result = evaluatePostfix(postfixTokens, currentCell);

            System.out.println("DEBUG: Formula result: " + result);
            return result;

        } catch (Exception e) {
            System.out.println("DEBUG: Formula evaluation failed: " + e.getMessage());
            throw new InvalidFormulaException("Error evaluating formula: " + formula, e.getMessage());
        }
    }

    // در FormulaEvaluator - اضافه کردن پشتیبانی از توابع در evaluatePostfix
    private Object evaluatePostfix(List<String> postfixTokens, String currentCell) {
        Stack<Object> valueStack = new Stack<>();

        for (String token : postfixTokens) {
            System.out.println("DEBUG: Processing token: " + token + ", Stack: " + valueStack);

            if (MathHelper.isNumber(token)) {
                valueStack.push(MathHelper.parseNumber(token));
            } else if (MathHelper.isConstant(token)) {
                double constantValue = MathHelper.getConstantValue(token);
                valueStack.push(constantValue);
            } else if (isCellReference(token)) {
                double cellValue = getCellValue(token, currentCell);
                valueStack.push(cellValue);
            } else if (ExpressionParser.isAggregateFunction(token)) {
                // پردازش توابع تجمعی
                String result = ExpressionParser.parseAggregateFunction(token, spreadsheet);
                valueStack.push(Double.parseDouble(result));
            } else if (MathHelper.isUnaryOrPostfixOperator(token)) {
                // پردازش عملگرهای unary و postfix
                if (valueStack.isEmpty()) {
                    throw new InvalidFormulaException("Insufficient operands for operator: " + token);
                }
                Object operand = valueStack.pop();
                if (!(operand instanceof Double)) {
                    throw new InvalidFormulaException("Operator " + token + " requires numeric operand");
                }
                double result = MathHelper.applyUnaryOrPostfixOperator(token, (Double) operand);
                valueStack.push(result);
            } else if (token.startsWith("\"") && token.endsWith("\"")) {
                String textValue = token.substring(1, token.length() - 1);
                valueStack.push(textValue);
            } else {
                // عملگر باینری
                if (valueStack.size() < 2) {
                    throw new InvalidFormulaException("Insufficient operands for operator: " + token);
                }

                Object b = valueStack.pop();
                Object a = valueStack.pop();

                if (!(a instanceof Double) || !(b instanceof Double)) {
                    throw new InvalidFormulaException("Binary operators require numeric operands");
                }

                double result = MathHelper.applyOperation(token.charAt(0), (Double) a, (Double) b);
                valueStack.push(result);
            }
        }

        if (valueStack.size() != 1) {
            throw new InvalidFormulaException("Invalid expression evaluation");
        }

        return valueStack.pop();
    }

    private double getCellValue(String cellReference, String currentCell) {
        String normalizedRef = normalizeCellReference(cellReference);

        // بررسی self-reference
        if (normalizedRef.equals(currentCell)) {
            throw new InvalidFormulaException("Self-reference detected: " + cellReference);
        }

        Cell cell = spreadsheet.getCell(normalizedRef);
        if (cell == null) {
            throw new InvalidReferenceException("Cell not found: " + normalizedRef);
        }

        if (cell.hasError()) {
            throw new InvalidReferenceException(
                    "Cell " + normalizedRef + " has error: " + cell.getErrorMessage()
            );
        }

        try {
            double value = cell.getNumericValue();
            System.out.println("DEBUG: Cell " + normalizedRef + " value: " + value);
            return value;
        } catch (IllegalStateException e) {
            throw new InvalidReferenceException(
                    "Cell " + normalizedRef + " does not contain numeric value: " + e.getMessage()
            );
        }
    }

    private boolean isCellReference(String value) {
        return value.matches("[A-Za-z]\\d+");
    }

    private String normalizeCellReference(String cellReference) {
        return cellReference.toUpperCase();
    }

    public void updateCellFormula(Cell cell, String formula, String currentCellRef) {
        try {
            Object result = evaluateFormula(formula, currentCellRef);
            cell.setComputedValue(result);
            cell.clearError();
            System.out.println("DEBUG: Cell formula updated successfully");
        } catch (Exception e) {
            cell.setComputedValue(null);
            System.out.println("DEBUG: Cell formula update failed: " + e.getMessage());
            throw e;
        }
    }
}
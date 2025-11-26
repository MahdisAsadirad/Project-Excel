package org.example.controller;

import org.example.exceptions.InvalidFormulaException;
import org.example.exceptions.InvalidReferenceException;
import org.example.model.Cell;
import org.example.model.Spreadsheet;
import org.example.model.Stack;
import org.example.utils.AggregateFunctions;
import org.example.utils.MathHelper;
import org.example.utils.Validationformula;

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

    private Object evaluatePostfix(List<String> postfixTokens, String currentCell) {
        Stack<Object> valueStack = new Stack<>();

        for (String token : postfixTokens) {
            System.out.println("DEBUG: Processing token: " + token + ", Stack: " + valueStack);

            token = token.toUpperCase();

            if (MathHelper.isNumber(token)) {
                valueStack.push(MathHelper.parseNumber(token));
            } else if (ExpressionParser.isAggregateFunction(token)) {
                double result = evaluateAggregateFunction(token);
                valueStack.push(result);
            } else if (MathHelper.isConstant(token)) {
                valueStack.push(MathHelper.getConstantValue(token));
            } else if (ExpressionParser.isCellReference(token)) {
                valueStack.push(getCellValue(token, currentCell));
            } else if (MathHelper.isUnaryOrPostfixOperator(token)) {
                // عملگرهای یوناری و پستفیکس
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
                valueStack.push(token.substring(1, token.length() - 1));
            } else {
                // عملگرهای باینری
                if (valueStack.size() < 2) {
                    throw new InvalidFormulaException("Insufficient operands for binary operator: " + token);
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
            throw new InvalidFormulaException("Invalid expression evaluation - stack has " + valueStack.size() + " items");
        }

        Object result = valueStack.pop();
        System.out.println("DEBUG: Final result: " + result);
        return result;
    }

    private double evaluateAggregateFunction(String functionCall) {
        String upperCall = functionCall.toUpperCase();

        int parenStart = upperCall.indexOf('(');
        int parenEnd = upperCall.lastIndexOf(')');

        if (parenStart == -1 || parenEnd == -1) {
            throw new IllegalArgumentException("Invalid function call: " + functionCall);
        }

        String functionName = upperCall.substring(0, parenStart); // SUM
        String range = upperCall.substring(parenStart + 1, parenEnd); // A1:A3

        // چک محدوده واقعی
        if (!Validationformula.isValidRange(range)) {
            throw new IllegalArgumentException("Invalid range format: " + range);
        }

        switch (functionName) {
            case "SUM":
                return AggregateFunctions.sum(spreadsheet, range);
            case "AVG":
                return AggregateFunctions.average(spreadsheet, range);
            case "MAX":
                return AggregateFunctions.max(spreadsheet, range);
            case "MIN":
                return AggregateFunctions.min(spreadsheet, range);
            case "COUNT":
                return AggregateFunctions.count(spreadsheet, range);
            default:
                throw new IllegalArgumentException("Unknown function: " + functionName);
        }
    }


    private boolean isValidRange(String range) {
        return range.matches("[A-Za-z]\\d+:[A-Za-z]\\d+");
    }

    private double getCellValue(String cellReference, String currentCell) {
        String normalizedRef = cellReference.toUpperCase();

        if (normalizedRef.equals(currentCell)) {
            throw new InvalidFormulaException("Self-reference detected: " + cellReference);
        }

        Cell cell = spreadsheet.getCell(normalizedRef);
        if (cell == null) {
            throw new InvalidReferenceException("Cell not found: " + normalizedRef);
        }

        if (cell.hasError()) {
            throw new InvalidReferenceException("Cell " + normalizedRef + " has error: " + cell.getErrorMessage());
        }

        try {
            return cell.getNumericValue();
        } catch (IllegalStateException e) {
            throw new InvalidReferenceException("Cell " + normalizedRef + " does not contain numeric value: " + e.getMessage());
        }
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

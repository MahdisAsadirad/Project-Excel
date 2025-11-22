package org.example.excel.controller;

import org.example.excel.exceptions.InvalidFormulaException;
import org.example.excel.model.Operator;
import org.example.excel.model.Stack;
import org.example.excel.utils.MathHelper;
import org.example.excel.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;

public class ExpressionParser {

    public static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (Character.isWhitespace(c)) {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                continue;
            }

            if (Operator.isOperator(c) || c == '(' || c == ')') {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else if (Character.isLetterOrDigit(c) || c == '.' || c == '@') {
                currentToken.append(c);
            } else {
                throw new InvalidFormulaException("Invalid character in expression: " + c);
            }
        }

        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }

        return tokens;
    }

    public static List<String> infixToPostfix(String infixExpression) {
        ValidationUtils.validateFormula(infixExpression);
        List<String> tokens = tokenize(infixExpression);
        List<String> postfix = new ArrayList<>();
        Stack<Character> operatorStack = new Stack<>();

        for (String token : tokens) {
            if (isOperand(token)) {
                postfix.add(token);
            } else if (token.equals("(")) {
                operatorStack.push('(');
            } else if (token.equals(")")) {
                while (!operatorStack.isEmpty() && operatorStack.peek() != '(') {
                    postfix.add(String.valueOf(operatorStack.pop()));
                }
                if (!operatorStack.isEmpty() && operatorStack.peek() == '(') {
                    operatorStack.pop();
                } else {
                    throw new InvalidFormulaException("Mismatched parentheses");
                }
            } else if (isOperator(token)) {
                char currentOp = token.charAt(0);
                while (!operatorStack.isEmpty() &&
                        operatorStack.peek() != '(' &&
                        hasHigherPrecedence(operatorStack.peek(), currentOp)) {
                    postfix.add(String.valueOf(operatorStack.pop()));
                }
                operatorStack.push(currentOp);
            }
        }

        while (!operatorStack.isEmpty()) {
            if (operatorStack.peek() == '(') {
                throw new InvalidFormulaException("Mismatched parentheses");
            }
            postfix.add(String.valueOf(operatorStack.pop()));
        }

        return postfix;
    }

    private static boolean isOperand(String token) {
        return MathHelper.isNumber(token) ||
                MathHelper.isConstant(token) ||
                isCellReference(token);
    }

    private static boolean isOperator(String token) {
        return token.length() == 1 && Operator.isOperator(token.charAt(0));
    }

    private static boolean hasHigherPrecedence(char op1, char op2) {
        return Operator.fromSymbol(op1).getPrecedence() >=
                Operator.fromSymbol(op2).getPrecedence();
    }

    private static boolean isCellReference(String token) {
        return token.matches("[A-Za-z]@?\\d+");
    }

    public static void validateTokens(List<String> tokens) {
        int operandCount = 0;
        int operatorCount = 0;

        for (String token : tokens) {
            if (isOperand(token)) {
                operandCount++;
            } else if (isOperator(token)) {
                operatorCount++;
            }
        }

        if (operandCount != operatorCount + 1) {
            throw new InvalidFormulaException(
                    "Invalid operand/operator count. Operands: " + operandCount +
                            ", Operators: " + operatorCount
            );
        }
    }
}

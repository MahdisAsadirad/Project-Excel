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
        boolean lastWasOperator = true; // برای تشخیص عملگرهای unary

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

                // تشخیص عملگرهای unary (+ و -)
                if ((c == '+' || c == '-') && (lastWasOperator || i == 0)) {
                    tokens.add("u" + c); // علامت unary
                } else {
                    tokens.add(String.valueOf(c));
                    lastWasOperator = (c != ')');
                }
            } else if (Character.isLetterOrDigit(c) || c == '.' || c == '@') {
                currentToken.append(c);
                lastWasOperator = false;
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
        Stack<String> operatorStack = new Stack<>();

        for (String token : tokens) {
            if (isOperand(token)) {
                postfix.add(token);
            } else if (token.equals("(")) {
                operatorStack.push(token);
            } else if (token.equals(")")) {
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    postfix.add(operatorStack.pop());
                }
                if (!operatorStack.isEmpty() && operatorStack.peek().equals("(")) {
                    operatorStack.pop();
                } else {
                    throw new InvalidFormulaException("Mismatched parentheses");
                }
            } else {
                // عملگر
                while (!operatorStack.isEmpty() &&
                        !operatorStack.peek().equals("(") &&
                        hasHigherPrecedence(operatorStack.peek(), token)) {
                    postfix.add(operatorStack.pop());
                }
                operatorStack.push(token);
            }
        }

        while (!operatorStack.isEmpty()) {
            if (operatorStack.peek().equals("(")) {
                throw new InvalidFormulaException("Mismatched parentheses");
            }
            postfix.add(operatorStack.pop());
        }

        return postfix;
    }

    private static boolean isOperand(String token) {
        return MathHelper.isNumber(token) ||
                MathHelper.isConstant(token) ||
                isCellReference(token) ||
                token.startsWith("u"); // عملگرهای unary
    }

    private static boolean isOperator(String token) {
        if (token.startsWith("u")) {
            return true; // عملگرهای unary
        }
        return token.length() == 1 && Operator.isOperator(token.charAt(0));
    }

    private static boolean hasHigherPrecedence(String op1, String op2) {
        return getPrecedence(op1) >= getPrecedence(op2);
    }

    private static int getPrecedence(String op) {
        if (op.startsWith("u")) {
            return 3; // اولویت بالا برای عملگرهای unary
        }

        if (op.length() == 1) {
            return Operator.fromSymbol(op.charAt(0)).getPrecedence();
        }
        return 0;
    }

    private static boolean isCellReference(String token) {
        return token.matches("[A-Za-z]@?\\d+");
    }

    public static void validateTokens(List<String> tokens) {
        // این متد می‌تواند برای اعتبارسنجی پیشرفته‌تر استفاده شود
        // فعلاً غیرفعال می‌کنیم چون با عملگرهای unary سازگار نیست
    }
}
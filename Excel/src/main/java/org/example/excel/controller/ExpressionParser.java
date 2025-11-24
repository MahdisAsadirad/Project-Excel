// ExpressionParser.java - بخش‌های بهبود یافته
package org.example.excel.controller;

// import statements...

import org.example.excel.exceptions.InvalidFormulaException;
import org.example.excel.model.Operator;
import org.example.excel.model.Spreadsheet;
import org.example.excel.model.Stack;
import org.example.excel.utils.AggregateFunctions;
import org.example.excel.utils.MathHelper;
import org.example.excel.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExpressionParser {

    // متد tokenize موجود - فقط بخش عملگر فاکتوریل را اضافه می‌کنیم
    public static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean lastWasOperator = true; // برای تشخیص عملگرهای unary
        boolean inText = false;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            // نادیده گرفتن فضاهای خالی
            if (Character.isWhitespace(c)) {
                if (currentToken.length() > 0 && !inText) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                continue;
            }

            // مدیریت متن درون کوتیشن
            if (c == '"') {
                inText = !inText;
                currentToken.append(c);
                continue;
            }

            if (inText) {
                currentToken.append(c);
                continue;
            }

            // تشخیص عملگر فاکتوریل (postfix)
            if (c == '!' && (i == 0 || expression.charAt(i-1) != '!')) {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                tokens.add("!");
                lastWasOperator = true;
                continue;
            }

            if (Operator.isOperator(c) || c == '(' || c == ')') {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }

                // تشخیص عملگرهای unary (+ و -)
                if ((c == '+' || c == '-') && (lastWasOperator || i == 0 || expression.charAt(i-1) == '(')) {
                    tokens.add("u" + c); // علامت unary
                    lastWasOperator = true;
                } else {
                    tokens.add(String.valueOf(c));
                    lastWasOperator = (c != ')');
                }
            } else if (Character.isLetterOrDigit(c) || c == '.' || c == '_') {
                currentToken.append(c);
                lastWasOperator = false;
            } else {
                throw new InvalidFormulaException("Invalid character in expression: '" + c + "'");
            }
        }

        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }

        return tokens;
    }

    // الگوریتم infixToPostfix بهبود یافته برای پشتیبانی از عملگر postfix
    public static List<String> infixToPostfix(String infixExpression) {
        ValidationUtils.validateFormula(infixExpression);
        List<String> tokens = tokenize(infixExpression);
        List<String> postfix = new ArrayList<>();
        Stack<String> operatorStack = new Stack<>();

        System.out.println("DEBUG: Tokens: " + tokens);

        for (String token : tokens) {
            if (isOperand(token)) {
                postfix.add(token);

                // برای عملگرهای postfix مانند فاکتوریل، بلافاصله بعد از عملوند اضافه می‌شوند
                while (!operatorStack.isEmpty() && MathHelper.isPostfixOperator(operatorStack.peek())) {
                    postfix.add(operatorStack.pop());
                }
            } else if (token.equals("(")) {
                operatorStack.push(token);
            } else if (token.equals(")")) {
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    postfix.add(operatorStack.pop());
                }
                if (operatorStack.isEmpty() || !operatorStack.peek().equals("(")) {
                    throw new InvalidFormulaException("Mismatched parentheses");
                }
                operatorStack.pop(); // حذف '('

                // بعد از بستن پرانتز، عملگرهای postfix را بررسی کن
                while (!operatorStack.isEmpty() && MathHelper.isPostfixOperator(operatorStack.peek())) {
                    postfix.add(operatorStack.pop());
                }
            } else {
                // عملگر (باینری، unary یا postfix)
                if (MathHelper.isPostfixOperator(token)) {
                    // عملگر postfix (مانند !) - روی پشته می‌ماند تا عملوند پردازش شود
                    operatorStack.push(token);
                } else {
                    // عملگرهای باینری و unary
                    while (!operatorStack.isEmpty() &&
                            !operatorStack.peek().equals("(") &&
                            hasHigherPrecedence(operatorStack.peek(), token)) {
                        postfix.add(operatorStack.pop());
                    }
                    operatorStack.push(token);
                }
            }
        }

        // اضافه کردن باقی‌مانده عملگرها
        while (!operatorStack.isEmpty()) {
            if (operatorStack.peek().equals("(")) {
                throw new InvalidFormulaException("Mismatched parentheses");
            }
            postfix.add(operatorStack.pop());
        }

        System.out.println("DEBUG: Postfix: " + postfix);
        return postfix;
    }

    // متدهای کمکی بدون تغییر...
    private static boolean hasHigherPrecedence(String op1, String op2) {
        int prec1 = getPrecedence(op1);
        int prec2 = getPrecedence(op2);

        if (prec1 > prec2) {
            return true;
        }

        if (prec1 == prec2) {
            return !MathHelper.isUnaryOrPostfixOperator(op1);
        }

        return false;
    }

    private static int getPrecedence(String op) {
        if (MathHelper.isUnaryOrPostfixOperator(op)) {
            if ("!".equals(op)) {
                return 4; // بالاترین اولویت برای فاکتوریل
            }
            return 4; // اولویت بالا برای عملگرهای unary
        }

        if (op.length() == 1) {
            char c = op.charAt(0);
            if (Operator.isOperator(c)) {
                return Operator.fromSymbol(c).getPrecedence();
            }
        }
        return 0;
    }

    private static boolean isOperand(String token) {
        return MathHelper.isNumber(token) ||
                MathHelper.isConstant(token) ||
                isCellReference(token) ||
                (token.startsWith("\"") && token.endsWith("\"")); // متن
    }

    private static boolean isCellReference(String token) {
        return token.matches("[A-Za-z]\\d+");
    }


    public static boolean isAggregateFunction(String token) {
        if (token == null) return false;
        String upperToken = token.toUpperCase();
        return upperToken.startsWith("SUM(") ||
                upperToken.startsWith("AVG(") ||
                upperToken.startsWith("MAX(") ||
                upperToken.startsWith("MIN(") ||
                upperToken.startsWith("COUNT(");
    }

    // متد جدید برای تجزیه توابع تجمعی
    public static String parseAggregateFunction(String functionCall, Spreadsheet spreadsheet) {
        if (functionCall == null || !functionCall.endsWith(")")) {
            throw new IllegalArgumentException("Invalid function call: " + functionCall);
        }

        String upperCall = functionCall.toUpperCase();
        String functionName = upperCall.substring(0, upperCall.indexOf('('));
        String range = upperCall.substring(upperCall.indexOf('(') + 1, upperCall.length() - 1);

        switch (functionName) {
            case "SUM":
                return String.valueOf(AggregateFunctions.sum(spreadsheet, range));
            case "AVG":
                return String.valueOf(AggregateFunctions.average(spreadsheet, range));
            case "MAX":
                return String.valueOf(AggregateFunctions.max(spreadsheet, range));
            case "MIN":
                return String.valueOf(AggregateFunctions.min(spreadsheet, range));
            case "COUNT":
                return String.valueOf(AggregateFunctions.count(spreadsheet, range));
            default:
                throw new IllegalArgumentException("Unknown function: " + functionName);
        }
    }

    public static Set<String> extractCellReferences(String formula) {
        Set<String> references = new HashSet<>();
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
                    if (isCellReference(ref)) {
                        references.add(ref.toUpperCase());
                    }
                    currentRef.setLength(0);
                }
                inReference = false;
            }
        }

        // بررسی آخرین reference
        if (inReference && currentRef.length() > 0) {
            String ref = currentRef.toString();
            if (isCellReference(ref)) {
                references.add(ref.toUpperCase());
            }
        }

        return references;
    }
}
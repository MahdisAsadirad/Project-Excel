package org.example.controller;

import org.example.exceptions.InvalidFormulaException;
import org.example.model.Cell;
import org.example.model.Operator;
import org.example.model.Spreadsheet;
import org.example.model.Stack;
import org.example.utils.MathHelper;
import org.example.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionParser {

    public static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean lastWasOperator = true;
        boolean inText = false;
        boolean inFunction = false;
        int parenCount = 0;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (Character.isWhitespace(c)) continue;

            if (c == '"') {
                inText = !inText;
                currentToken.append(c);
                continue;
            }

            if (inText) {
                currentToken.append(c);
                continue;
            }

            // شناسایی توابع تجمعی
            if (!inFunction && currentToken.length() == 0 && Character.isLetter(c)) {
                currentToken.append(c);
                continue;
            }

            if (!inFunction && currentToken.length() > 0 && Character.isLetter(c)) {
                currentToken.append(c);
                String potentialFunction = currentToken.toString().toUpperCase();
                if (potentialFunction.matches("(SUM|AVG|MAX|MIN|COUNT)")) {
                    inFunction = true;
                }
                continue;
            }

            if (inFunction) {
                currentToken.append(c);
                if (c == '(') parenCount++;
                else if (c == ')') parenCount--;

                if (parenCount == 0) {
                    tokens.add(currentToken.toString()); // کل تابع به یک توکن
                    currentToken.setLength(0);
                    inFunction = false;
                    lastWasOperator = false;
                }
                continue;
            }

            // عملیات و پرانتزها
            if (Operator.isOperator(c) || c == '(' || c == ')') {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }

                if (c == '(') {
                    tokens.add("(");
                    lastWasOperator = true;
                } else if (c == ')') {
                    tokens.add(")");
                    lastWasOperator = false;
                } else if ((c == '+' || c == '-') && (lastWasOperator || i == 0)) {
                    tokens.add("U" + c);
                    lastWasOperator = true;
                } else {
                    tokens.add(String.valueOf(c));
                    lastWasOperator = true;
                }
            } else {
                currentToken.append(c);
                lastWasOperator = false;
            }
        }

        if (currentToken.length() > 0) tokens.add(currentToken.toString());
        return tokens;
    }

    public static List<String> infixToPostfix(String infixExpression) {
        ValidationUtils.validateFormula(infixExpression);
        List<String> tokens = tokenize(infixExpression);
        List<String> postfix = new ArrayList<>();
        Stack<String> operatorStack = new Stack<>();

        System.out.println("DEBUG: Tokens: " + tokens);

        for (String token : tokens) {
            if (isOperand(token) || isAggregateFunction(token)) {
                postfix.add(token);
            }  else if (token.equals("(")) {
                operatorStack.push(token);
            } else if (token.equals(")")) {
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    postfix.add(operatorStack.pop());
                }
                if (operatorStack.isEmpty() || !operatorStack.peek().equals("(")) {
                    throw new InvalidFormulaException("Mismatched parentheses");
                }
                operatorStack.pop(); // حذف '('

                // اگر تابع در استک است، آن را اضافه کن
                if (!operatorStack.isEmpty() && isAggregateFunction(operatorStack.peek())) {
                    postfix.add(operatorStack.pop());
                }
            } else {
                if (MathHelper.isPostfixOperator(token)) {
                    postfix.add(token);
                } else {
                    while (!operatorStack.isEmpty() &&
                            !operatorStack.peek().equals("(") &&
                            hasHigherPrecedence(operatorStack.peek(), token)) {
                        postfix.add(operatorStack.pop());
                    }
                    operatorStack.push(token);
                }
            }
        }

        while (!operatorStack.isEmpty()) {
            if (operatorStack.peek().equals("(")) {
                throw new InvalidFormulaException("Mismatched parentheses");
            }
            postfix.add(operatorStack.pop());
        }

        System.out.println("DEBUG: Postfix: " + postfix);
        return postfix;
    }

    private static boolean hasHigherPrecedence(String op1, String op2) {
        int prec1 = getPrecedence(op1);
        int prec2 = getPrecedence(op2);

        if (prec1 > prec2) {
            return true;
        }

        if (prec1 == prec2) {
            return isLeftAssociative(op1);
        }

        return false;
    }

    private static boolean isLeftAssociative(String op) {
        return !MathHelper.isUnaryOrPostfixOperator(op);
    }

    private static int getPrecedence(String op) {
        if (MathHelper.isUnaryOrPostfixOperator(op)) {
            if ("!".equals(op)) {
                return 4;
            }
            return 3;
        }

        if (op.length() == 1) {
            char c = op.charAt(0);
            if (Operator.isOperator(c)) {
                return Operator.fromSymbol(c).getPrecedence();
            }
        }

        if (isAggregateFunction(op)) {
            return 5;
        }

        return 0;
    }

    private static boolean isOperand(String token) {
        return MathHelper.isNumber(token) ||
                MathHelper.isConstant(token) ||
                isCellReference(token) ||
                isRangeReference(token) ||
                (token.startsWith("\"") && token.endsWith("\""));
    }

    public static boolean isCellReference(String token) {
        return token.matches("[A-Za-z]\\d+");
    }

    public static boolean isRangeReference(String token) {
        return token.matches("[A-Za-z]\\d+:[A-Za-z]\\d+");
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

    public static Set<String> extractCellReferences(String formula) {
        Set<String> references = new HashSet<>();

        // الگو برای شناسایی سلول‌های منفرد (A1, B2, etc.)
        Pattern singleCellPattern = Pattern.compile("[A-Za-z]\\d+");
        Matcher singleMatcher = singleCellPattern.matcher(formula);

        while (singleMatcher.find()) {
            references.add(singleMatcher.group().toUpperCase());
        }

        // الگو برای شناسایی محدوده‌ها در توابع تجمعی (A1:B5)
        Pattern rangePattern = Pattern.compile("([A-Za-z]\\d+):([A-Za-z]\\d+)");
        Matcher rangeMatcher = rangePattern.matcher(formula);

        while (rangeMatcher.find()) {
            String startCell = rangeMatcher.group(1).toUpperCase();
            String endCell = rangeMatcher.group(2).toUpperCase();

            // استخراج تمام سلول‌های موجود در محدوده
            references.addAll(extractCellsInRange(startCell, endCell));
        }

        return references;
    }

    private static Set<String> extractCellsInRange(String startCell, String endCell) {
        Set<String> cells = new HashSet<>();

        int startCol = startCell.charAt(0) - 'A';
        int startRow = Integer.parseInt(startCell.substring(1)) - 1;
        int endCol = endCell.charAt(0) - 'A';
        int endRow = Integer.parseInt(endCell.substring(1)) - 1;

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                String cellRef = "" + (char)('A' + col) + (row + 1);
                cells.add(cellRef);
            }
        }

        return cells;
    }

    public static List<Double> getValuesFromRange(String range, Spreadsheet spreadsheet) {
        String[] parts = range.split(":");
        if (parts.length != 2) throw new IllegalArgumentException("Invalid range: " + range);

        String start = parts[0].toUpperCase();
        String end = parts[1].toUpperCase();

        int startCol = start.charAt(0) - 'A';
        int startRow = Integer.parseInt(start.substring(1)) - 1;
        int endCol = end.charAt(0) - 'A';
        int endRow = Integer.parseInt(end.substring(1)) - 1;

        List<Double> values = new ArrayList<>();
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                String cellName = "" + (char)('A' + c) + (r + 1);
                Cell cell = spreadsheet.getCell(cellName);
                if (cell != null && cell.getComputedValue() instanceof Number) {
                    double val = ((Number)cell.getComputedValue()).doubleValue();
                    values.add(val);
                } else {
                    // اگر سلول خالی است یا مقدار عددی ندارد، صفر در نظر بگیر
                    values.add(0.0);
                }
            }
        }
        return values;
    }

    // متد جدید برای استخراج محدوده از تابع تجمعی
    public static String extractRangeFromFunction(String functionCall) {
        if (!isAggregateFunction(functionCall)) {
            throw new IllegalArgumentException("Not an aggregate function: " + functionCall);
        }

        int start = functionCall.indexOf('(') + 1;
        int end = functionCall.lastIndexOf(')');
        return functionCall.substring(start, end);
    }


    public static boolean isFillCommand(String command) {
        if (command == null) return false;
        String upperCommand = command.toUpperCase().trim();

        // الگوهای مختلف برای دستور Fill
        String pattern1 = "^FILL\\s*\\(\\s*[A-Za-z]\\d+\\s*,\\s*[A-Za-z]\\d+\\s*:\\s*[A-Za-z]\\d+\\s*\\)$";
        String pattern2 = "^FILL\\s*\\(\\s*[A-Za-z]\\d+\\s*,\\s*[A-Za-z]\\d+\\s*:\\s*[A-Za-z]\\d+\\s*\\)$";

        return upperCommand.matches(pattern1) || upperCommand.matches(pattern2);
    }

    public static String[] parseFillCommand(String command) {
        if (!isFillCommand(command)) {
            throw new IllegalArgumentException("Invalid FILL command format: " + command);
        }

        // حذف FILL و پرانتزها و فضاهای اضافه
        String cleanCommand = command.toUpperCase().replace("FILL", "").trim();

        // حذف پرانتزها
        if (cleanCommand.startsWith("(") && cleanCommand.endsWith(")")) {
            cleanCommand = cleanCommand.substring(1, cleanCommand.length() - 1).trim();
        }

        // جدا کردن source و target
        String[] parts = cleanCommand.split("\\s*,\\s*");
        if (parts.length != 2) {
            throw new IllegalArgumentException("FILL command requires exactly 2 parameters: source and range");
        }

        String sourceCell = parts[0].trim();
        String targetRange = parts[1].trim();

        // نرمالایز کردن
        sourceCell = sourceCell.toUpperCase();
        targetRange = targetRange.toUpperCase();

        System.out.println("DEBUG: Parsed Fill - Source: " + sourceCell + ", Range: " + targetRange);

        return new String[]{sourceCell, targetRange};
    }
}

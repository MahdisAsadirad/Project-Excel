package org.example.controller;

import org.example.exceptions.InvalidFormulaException;
import org.example.model.Cell;
import org.example.model.Operator;
import org.example.model.Spreadsheet;
import org.example.model.Stack;
import org.example.utils.MathHelper;
import org.example.utils.Validationformula;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Calculate {

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
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                    inFunction = false;
                    lastWasOperator = false;
                }
                continue;
            }

            if (Operator.isOperator(c) || c == '(' || c == ')' || c == ',') {
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
                } else if (c == ',') {
                    tokens.add(",");
                    lastWasOperator = true;
                } else if ((c == '+' || c == '-') && (lastWasOperator || i == 0)) {
                    // عملگر یوناری
                    tokens.add("U" + c);
                    lastWasOperator = true;
                } else {
                    // عملگر باینری
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
        Validationformula.validateFormula(infixExpression);
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
                if (operatorStack.isEmpty() || !operatorStack.peek().equals("(")) {
                    throw new InvalidFormulaException("Mismatched parentheses");
                }
                operatorStack.pop(); // حذف '('
            } else if (token.startsWith("U")) {
                // عملگرهای یوناری اولویت بالایی دارند
                operatorStack.push(token);
            } else {
                // عملگرهای باینری
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

    private static boolean hasHigherPrecedence(String op1, String op2) {
        int prec1 = getPrecedence(op1);
        int prec2 = getPrecedence(op2);

        return prec1 > prec2 || (prec1 == prec2 && isLeftAssociative(op1));
    }

    private static int getPrecedence(String op) {
        if (op.startsWith("U")) {
            return 4; // بالاترین اولویت برای عملگرهای یوناری
        }

        switch (op) {
            case "^": return 3;
            case "*": case "/": return 2;
            case "+": case "-": return 1;
            default: return 0;
        }
    }

    private static boolean isLeftAssociative(String op) {
        return !op.equals("^"); // توان از راست به چپ است
    }

    private static boolean isOperand(String token) {
        return MathHelper.isNumber(token) ||
                MathHelper.isConstant(token) ||
                isCellReference(token) ||
                isRangeReference(token) ||
                isAggregateFunction(token) ||
                ((token.startsWith("\"") && token.endsWith("\"")) || (token.startsWith("”") && token.endsWith("”")) );
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
                    // اگر سلول خالیه یا مقدار عددی ندارد صفر در نظر بگیر
                    values.add(0.0);
                }
            }
        }
        return values;
    }
}

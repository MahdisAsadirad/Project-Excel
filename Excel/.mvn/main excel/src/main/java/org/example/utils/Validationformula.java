package org.example.utils;

public class Validationformula {
    public static boolean isFormula(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        String trimmed = content.trim();
        boolean result = trimmed.startsWith("=") && trimmed.length() > 1;

        return result;
    }

    public static String extractFormula(String content) {
        if (!isFormula(content)) {
            throw new IllegalArgumentException("Not a valid formula: " + content);
        }
        return content.substring(1).trim();
    }

    public static void validateFormula(String formula) {
        if (formula == null || formula.trim().isEmpty()) {
            throw new IllegalArgumentException("Formula cannot be empty");
        }

        validateParentheses(formula);
        validateOperatorSequence(formula);
    }

    private static void validateParentheses(String formula) {
        int balance = 0;
        for (char c : formula.toCharArray()) {
            if (c == '(') {
                balance++;
            } else if (c == ')') {
                balance--;
                if (balance < 0) {
                    throw new IllegalArgumentException("Mismatched parentheses");
                }
            }
        }

        if (balance != 0) {
            throw new IllegalArgumentException("Mismatched parentheses");
        }
    }

    private static void validateOperatorSequence(String formula) {
        char[] chars = formula.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            char current = chars[i];
            char next = chars[i + 1];

            if (isBinaryOperator(current) && isBinaryOperator(next)) {
                if (next != '-') {
                    throw new IllegalArgumentException("Invalid operator sequence: " + current + next);
                }

                if (!isValidPositionForUnaryMinus(chars, i)) {
                    throw new IllegalArgumentException("Invalid operator before negative sign: " + current + next);
                }
            }
        }
    }

    private static boolean isValidPositionForUnaryMinus(char[] chars, int position) {
        char current = chars[position];

        if (position == 0) {
            return true;
        }

        if (current == '(' || current == ',') {
            return true; // منفی بعد از پرانتز باز یا کاما مجاز است
        }

        return isBinaryOperator(current);
    }

    private static boolean isBinaryOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == '=';
    }


    public static boolean isTextContent(String content) {
        return content != null && content.startsWith("\"") && content.endsWith("\"") && content.length() >= 2;
    }

    public static String extractTextContent(String content) {
        if (!isTextContent(content)) {
            throw new IllegalArgumentException("Not a valid text content: " + content);
        }
        return content.substring(1, content.length() - 1);
    }

    public static boolean isNumberContent(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(content);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidRange(String range) {
        if (range == null || range.trim().isEmpty()) {
            return false;
        }
        return range.matches("[A-Za-z]\\d+:[A-Za-z]\\d+");
    }
}
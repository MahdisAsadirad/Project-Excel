package org.example.excel.utils;

import org.example.excel.model.Operator;

public class ValidationUtils {

    public static boolean isFormula(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        String trimmed = content.trim();
        System.out.println("DEBUG: Checking formula for: '" + trimmed + "'"); // خط دیباگ

        boolean result = trimmed.startsWith("=") && trimmed.length() > 1;
        System.out.println("DEBUG: Is formula? " + result); // خط دیباگ

        return result;
    }

    public static String extractFormula(String content) {
        if (!isFormula(content)) {
            throw new IllegalArgumentException("Not a valid formula: " + content);
        }
        return content.substring(1).trim(); // حذف = و فضاهای اضافه
    }

    public static void validateFormula(String formula) {
        if (formula == null || formula.trim().isEmpty()) {
            throw new IllegalArgumentException("Formula cannot be empty");
        }

        // بررسی پرانتزها
        validateParentheses(formula);

        // بررسی توالی عملگرها
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

            if (Operator.isOperator(current) && Operator.isOperator(next) &&
                    current != '(' && next != ')' && current != ')' && next != '(') {
                throw new IllegalArgumentException("Invalid operator sequence: " + current + next);
            }
        }
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
}

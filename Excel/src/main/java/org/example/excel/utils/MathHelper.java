package org.example.excel.utils;

import org.example.excel.exceptions.DivisionByZeroException;

public class MathHelper {

    public static final double PI = 3.14159;
    public static final double E = 2.71828;

    public static double applyOperation(char operator, double a, double b) {
        switch (operator) {
            case '+':
                return a + b;
            case '-':
                return a - b;
            case '*':
                return a * b;
            case '/':
                if (b == 0) {
                    throw new DivisionByZeroException(a + " / " + b);
                }
                return a / b;
            case '^':
                return Math.pow(a, b);
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    public static boolean isConstant(String token) {
        return "PI".equals(token) || "E".equals(token);
    }

    public static double getConstantValue(String constant) {
        switch (constant.toUpperCase()) {
            case "PI":
                return PI;
            case "E":
                return E;
            default:
                throw new IllegalArgumentException("Unknown constant: " + constant);
        }
    }

    public static boolean isNumber(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static double parseNumber(String token) {
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + token);
        }
    }

    public static String formatNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.2f", value);
        }
    }
}

package org.example.utils;

import org.example.exceptions.DivisionByZeroException;
import org.example.exceptions.InvalidExponentiationException;
import org.example.exceptions.InvalidFactorialException;

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
                return safePower(a, b);
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    public static double applyUnaryOrPostfixOperator(String operator, double operand) {
        switch (operator) {
            case "U+":
                return +operand;
            case "U-":
                return -operand;
            case "!":
                return factorial(operand);
            default:
                throw new IllegalArgumentException("Unknown unary/postfix operator: " + operator);
        }
    }

    public static boolean isUnaryOrPostfixOperator(String token) {
        return "U+".equals(token) || "U-".equals(token) || "!".equals(token);
    }

    public static double factorial(double n) {
        if (n < 0) {
            throw new InvalidFactorialException("Factorial is not defined for negative numbers: " + n);
        }
        if (n != Math.floor(n)) {
            throw new InvalidFactorialException("Factorial is only defined for integers: " + n);
        }

        int intN = (int) n;
        double result = 1;
        for (int i = 2; i <= intN; i++) {
            result *= i;
        }
        return result;
    }

    public static double safePower(double base, double exponent) {
        if (base == 0 && exponent < 0) {
            throw new DivisionByZeroException("0 raised to negative power");
        }
        if (base < 0 && exponent != Math.floor(exponent)) {
            throw new InvalidExponentiationException("Negative base with fractional exponent");
        }
        return Math.pow(base, exponent);
    }

    public static boolean isConstant(String token) {
        if (token == null) return false;
        String upperToken = token.toUpperCase();
        return "PI".equals(upperToken) || "E".equals(upperToken);
    }

    public static double getConstantValue(String constant) {
        if (constant == null) {
            throw new IllegalArgumentException("Constant cannot be null");
        }

        String upperConstant = constant.toUpperCase();
        switch (upperConstant) {
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
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }

        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        } else {
            String formatted = String.format("%.2f", value);
            if (formatted.contains(".")) {
                formatted = formatted.replaceAll("0*$", "").replaceAll("\\.$", "");
            }
            return formatted;
        }
    }
}

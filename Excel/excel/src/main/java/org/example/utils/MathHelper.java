package org.example.utils;

import org.example.exceptions.DivisionByZeroException;
import org.example.exceptions.InvalidExponentiationException;
import org.example.exceptions.InvalidFactorialException;
import org.example.exceptions.InvalidSquareRootException;

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

    // متد جدید برای اعمال عملگرهای unary و postfix
    public static double applyUnaryOrPostfixOperator(String operator, double operand) {
        if (operator.equals("u+")) {
            return +operand;
        } else if (operator.equals("u-")) {
            return -operand;
        } else if (operator.equals("!")) {
            return factorial(operand);
        } else {
            throw new IllegalArgumentException("Unknown unary/postfix operator: " + operator);
        }
    }

    // محاسبه فاکتوریل
    public static double factorial(double n) {
        if (n < 0) {
            throw new InvalidFactorialException("Factorial is not defined for negative numbers: " + n);
        }
        if (n != Math.floor(n)) {
            throw new InvalidFactorialException("Factorial is only defined for integers: " + n);
        }
        if (n > 170) { // محدودیت double
            throw new InvalidFactorialException("Factorial value too large for double precision: " + n);
        }

        int intN = (int) n;
        double result = 1;
        for (int i = 2; i <= intN; i++) {
            result *= i;
            if (Double.isInfinite(result)) {
                throw new InvalidFactorialException("Factorial value exceeds maximum double value at: " + i);
            }
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

    public static double squareRoot(double value) {
        if (value < 0) {
            throw new InvalidSquareRootException("Square root is not defined for negative numbers: " + value);
        }
        return Math.sqrt(value);
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

    public static boolean isUnaryOrPostfixOperator(String token) {
        return "u+".equals(token) || "u-".equals(token) || "!".equals(token);
    }

    public static boolean isPostfixOperator(String token) {
        return "!".equals(token);
    }
}

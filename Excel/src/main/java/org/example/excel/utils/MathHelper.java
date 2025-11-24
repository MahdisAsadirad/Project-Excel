// MathHelper.java
package org.example.excel.utils;

import org.example.excel.exceptions.*;

public class MathHelper {

    public static final double PI = 3.14159;
    public static final double E = 2.71828;

    // متد بهبودیافته برای اعمال تمام عملیات
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

    // محاسبه ایمن توان
    private static double safePower(double base, double exponent) {
        if (base == 0 && exponent < 0) {
            throw new DivisionByZeroException("Zero raised to negative power");
        }
        if (base < 0 && exponent != Math.floor(exponent)) {
            throw new InvalidExponentiationException(
                    "Cannot raise negative base " + base + " to fractional exponent " + exponent
            );
        }
        return Math.pow(base, exponent);
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

    // محاسبه لگاریتم
    public static double logarithm(double value, double base) {
        if (value <= 0) {
            throw new InvalidLogarithmException("Logarithm is not defined for non-positive values: " + value);
        }
        if (base <= 0 || base == 1) {
            throw new InvalidLogarithmException("Logarithm base must be positive and not equal to 1: " + base);
        }
        return Math.log(value) / Math.log(base);
    }

    // محاسبه جذر
    public static double squareRoot(double value) {
        if (value < 0) {
            throw new InvalidSquareRootException("Square root is not defined for negative numbers: " + value);
        }
        return Math.sqrt(value);
    }

    // شناسایی ثابت‌ها
    public static boolean isConstant(String token) {
        if (token == null) return false;
        String upperToken = token.toUpperCase();
        return "PI".equals(upperToken) || "E".equals(upperToken);
    }

    // دریافت مقدار ثابت
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

    // شناسایی اعداد
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

    // تجزیه عدد
    public static double parseNumber(String token) {
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + token);
        }
    }

    // فرمت‌بندی عدد برای نمایش
    public static String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }

        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        } else {
            // فرمت تا دو رقم اعشار و حذف صفرهای اضافی
            String formatted = String.format("%.2f", value);
            if (formatted.contains(".")) {
                formatted = formatted.replaceAll("0*$", "").replaceAll("\\.$", "");
            }
            return formatted;
        }
    }

    // بررسی اینکه آیا یک رشته می‌تواند عملگر unary یا postfix باشد
    public static boolean isUnaryOrPostfixOperator(String token) {
        return "u+".equals(token) || "u-".equals(token) || "!".equals(token);
    }

    // بررسی اینکه آیا عملگر postfix است (فاکتوریل)
    public static boolean isPostfixOperator(String token) {
        return "!".equals(token);
    }
}
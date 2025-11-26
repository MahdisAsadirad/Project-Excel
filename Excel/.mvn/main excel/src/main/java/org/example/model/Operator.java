package org.example.model;

public enum Operator {
    ADDITION('+', 1),
    SUBTRACTION('-', 1),
    MULTIPLICATION('*', 2),
    DIVISION('/', 2),
    POWER('^', 3),
    FACTORIAL('!', 4),
    LEFT_PARENTHESIS('(', 0),
    RIGHT_PARENTHESIS(')', 0);

    private final char symbol;
    private final int precedence;

    Operator(char symbol, int precedence) {
        this.symbol = symbol;
        this.precedence = precedence;
    }

    public char getSymbol() {
        return symbol;
    }

    public int getPrecedence() {
        return precedence;
    }

    public static Operator fromSymbol(char symbol) {
        for (Operator op : values()) {
            if (op.symbol == symbol) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown operator: " + symbol);
    }

    public static boolean isOperator(char c) {
        for (Operator op : values()) {
            if (op.symbol == c) {
                return true;
            }
        }
        return false;
    }
    public static boolean isBinaryOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    public static boolean isUnaryOperator(char c) {
        return c == '!' || c == 'u' + '+' || c == 'u' + '-';
    }
}

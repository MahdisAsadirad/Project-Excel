package org.example.model;

public enum Operator {
    ADDITION('+'),
    SUBTRACTION('-'),
    MULTIPLICATION('*'),
    DIVISION('/'),
    POWER('^'),
    FACTORIAL('!'),
    LEFT_PARENTHESIS('('),
    RIGHT_PARENTHESIS(')');

    private final char symbol;

    Operator(char symbol) {
        this.symbol = symbol;
    }

    public static boolean isOperator(char c) {
        for (Operator op : values()) {
            if (op.symbol == c) {
                return true;
            }
        }
        return false;
    }
}

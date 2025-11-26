package org.example.model;

public enum ErrorType {
    NO_ERROR("No Error"),
    CIRCULAR_DEPENDENCY("Circular Dependency"),
    INVALID_REFERENCE("Invalid Reference"),
    DIVISION_BY_ZERO("Division by Zero"),
    INVALID_FORMULA("Invalid Formula"),
    VALUE_ERROR("Value Error"),
    SYNTAX_ERROR("Syntax Error"),
    SELF_REFERENCE("Self Reference"),
    INVALID_OPERATOR("Invalid Operator"),
    MISSING_OPERAND("Missing Operand");

    private final String description;

    ErrorType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

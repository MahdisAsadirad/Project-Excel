package org.example.model;
public enum CellType {
    EMPTY("--"),
    NUMBER("NUMBER"),
    TEXT("TEXT"),
    FORMULA("FORMULA"),
    ERROR("#ERR!");

    private final String displayName;

    CellType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

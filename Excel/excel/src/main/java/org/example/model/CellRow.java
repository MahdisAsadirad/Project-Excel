package org.example.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.example.model.Spreadsheet;
import org.example.model.Cell;
import org.example.utils.CellReferenceConverter;

public class CellRow {
    private final StringProperty[] cells;

    public CellRow(Spreadsheet sheet, int rowIndex) {
        cells = new StringProperty[sheet.getCols()];
        for (int col = 0; col < sheet.getCols(); col++) {
            Cell cell = sheet.getCell(CellReferenceConverter.toCellReference(rowIndex, col));
            cells[col] = new SimpleStringProperty(cell.toString());
        }
    }

    public StringProperty getCellProperty(int index) {
        return cells[index];
    }
}


package ru.portfolio.portfolio.parser;

import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.*;

public class ExcelTable implements Iterable<Row> {
    private final Sheet sheet;
    @Getter
    private final CellRangeAddress tableRange;
    private final Map<TableColumn, Integer> columnIndices;
    @Setter
    private int dataRowOffset = 2;
    @Getter
    private final boolean empty;
    
    public static ExcelTable of(Sheet sheet, String tableName, String tableFooterString,
                         Class<? extends TableColumnDescription> headerDescription) {
       return new ExcelTable(sheet, ExcelTableHelper.getTableCellRange(sheet, tableName, tableFooterString), headerDescription);
    }

    public static ExcelTable of(Sheet sheet, String tableName,
                         Class<? extends TableColumnDescription> headerDescription) {
        return new ExcelTable(sheet, ExcelTableHelper.getTableCellRange(sheet, tableName), headerDescription);
    }

    private ExcelTable(Sheet sheet, CellRangeAddress tableRange, Class<? extends TableColumnDescription> headerDescription) {
        this.sheet = sheet;
        this.tableRange = tableRange;
        this.empty = this.tableRange.equals(ExcelTableHelper.EMTPY_RANGE);
        this.columnIndices = empty ?
                Collections.emptyMap() :
                getColumnIndices(sheet, this.tableRange, headerDescription);
    }

    private  Map<TableColumn, Integer> getColumnIndices(Sheet sheet, CellRangeAddress tableRange,
                                                        Class<? extends TableColumnDescription> headerDescription) {
        Map<TableColumn, Integer> columnIndices = new HashMap<>();
        Row header = sheet.getRow(tableRange.getFirstRow() + 1);
        TableColumn[] columns = Arrays.stream(headerDescription.getEnumConstants())
                .map(TableColumnDescription::getColumn)
                .toArray(TableColumn[]::new);
        for (TableColumn column : columns) {
            columnIndices.put(column, column.getColumnIndex(header));
        }
        return columnIndices;
    }

    public Cell getCell(Row row, TableColumnDescription columnDescription) {
        return row.getCell(columnIndices.get(columnDescription.getColumn()));
    }

    @Override
    public Iterator<Row> iterator() {
        return new ExelTableIterator();
    }

    class ExelTableIterator implements Iterator<Row> {
        private int cnt = 0;

        @Override
        public boolean hasNext() {
            return cnt < (tableRange.getLastRow() - tableRange.getFirstRow() - dataRowOffset);
        }

        @Override
        public Row next() {
            Row row;
            do {
                row = sheet.getRow(tableRange.getFirstRow() + dataRowOffset + (cnt++));
            } while (row == null && hasNext());
            return row;
        }
    }
}

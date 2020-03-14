package ru.portfolio.portfolio.parser;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;

@Slf4j
@ToString(of = {"tableName"})
public class ExcelTable implements Iterable<Row> {
    private final Sheet sheet;
    private final String tableName;
    @Getter
    private final CellRangeAddress tableRange;
    private final Map<TableColumn, Integer> columnIndices;
    @Getter
    private final boolean empty;
    /**
     * Offset of first data row. First table row is a header. Default is 2.
     */
    @Setter
    private int dataRowOffset = 2;
    /**
     * Set to true if last table row contains total information. Default is false.
     */
    @Setter
    private boolean isLastTableRowContainsTotalData = false;

    public static ExcelTable of(Sheet sheet, String tableName, String tableFooterString,
                         Class<? extends TableColumnDescription> headerDescription) {
        ExcelTable table = new ExcelTable(sheet, tableName,
                ExcelTableHelper.getTableCellRange(sheet, tableName, tableFooterString),
                headerDescription);
        table.setLastTableRowContainsTotalData(true);
        return table;
    }

    public static ExcelTable of(Sheet sheet, String tableName,
                         Class<? extends TableColumnDescription> headerDescription) {
        ExcelTable table = new ExcelTable(sheet, tableName,
                ExcelTableHelper.getTableCellRange(sheet, tableName),
                headerDescription);
        table.setLastTableRowContainsTotalData(false);
        return table;
    }

    private ExcelTable(Sheet sheet, String tableName, CellRangeAddress tableRange, Class<? extends TableColumnDescription> headerDescription) {
        this.sheet = sheet;
        this.tableName = tableName;
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

    public <T> List<T> getData(Path file, BiFunction<ExcelTable, Row, T> rowExtractor) {
        List<T> data = new ArrayList<>();
        for (Row row : this) {
            if (row != null) {
                try {
                    T transaction = rowExtractor.apply(this, row);
                    if (transaction != null) {
                        data.add(transaction);
                    }
                } catch (Exception e) {
                    log.warn("Не могу распарсить таблицу '{}' в файле {}, строка {}", tableName, file.getFileName(), row.getRowNum(), e);
                }
            }
        }
        return data;
    }

    public <T> List<T> getDataCollection(Path file, BiFunction<ExcelTable, Row, Collection<T>> rowExtractor) {
        List<T> data = new ArrayList<>();
        for (Row row : this) {
            if (row != null) {
                try {
                    Collection<T> transaction = rowExtractor.apply(this, row);
                    if (transaction != null) {
                        data.addAll(transaction);
                    }
                } catch (Exception e) {
                    log.warn("Не могу распарсить таблицу '{}' в файле {}, строка {}", tableName, file.getFileName(), row.getRowNum(), e);
                }
            }
        }
        return data;
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
            int dataRowsCount = tableRange.getLastRow() - tableRange.getFirstRow()
                    - dataRowOffset
                    + (isLastTableRowContainsTotalData ? 0 : 1);
            return cnt < dataRowsCount;
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

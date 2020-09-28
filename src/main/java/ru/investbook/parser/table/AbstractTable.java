/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.parser.table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import ru.investbook.parser.TableColumn;
import ru.investbook.parser.TableColumnDescription;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import static java.util.Collections.emptyList;

@Slf4j
@ToString(of = {"tableName"})
public abstract class AbstractTable implements Table {

    protected final ReportPage reportPage;
    protected final String tableName;
    @Getter
    protected final TableCellRange tableRange;
    protected final Map<TableColumn, Integer> columnIndices;
    @Getter
    protected final boolean empty;
    /**
     * Offset of first data row. First table row is a header.
     */
    private final int dataRowOffset;
    /**
     * Set to true if last table row contains total information. Default is false.
     */
    @Setter
    private boolean isLastTableRowContainsTotalData = false;


    protected AbstractTable(ReportPage reportPage, String tableName, TableCellRange tableRange,
                       Class<? extends TableColumnDescription> headerDescription, int headersRowCount) {
        this.reportPage = reportPage;
        this.tableName = tableName;
        this.tableRange = tableRange;
        this.dataRowOffset = 1 + headersRowCount; // table_name + headersRowCount
        this.empty = tableRange.equals(TableCellRange.EMPTY_RANGE) ||
                ((tableRange.getLastRow() - tableRange.getFirstRow()) <= headersRowCount);
        this.columnIndices = empty ?
                Collections.emptyMap() :
                getColumnIndices(reportPage, tableRange, headerDescription, headersRowCount);
    }

    private Map<TableColumn, Integer> getColumnIndices(ReportPage reportPage, TableCellRange tableRange,
                                                       Class<? extends TableColumnDescription> headerDescription,
                                                       int headersRowCount) {
        Map<TableColumn, Integer> columnIndices = new HashMap<>();
        TableRow[] headerRows = new TableRow[headersRowCount];
        for (int i = 0; i < headersRowCount; i++) {
            headerRows[i] = reportPage.getRow(tableRange.getFirstRow() + 1 + i);
        }
        TableColumn[] columns = Arrays.stream(headerDescription.getEnumConstants())
                .map(TableColumnDescription::getColumn)
                .toArray(TableColumn[]::new);
        for (TableColumn column : columns) {
            columnIndices.put(column, column.getColumnIndex(headerRows));
        }
        return columnIndices;
    }

    /**
     * Extracts exactly one object from excel row
     */
    public <T> List<T> getData(Path file, BiFunction<? super Table, TableRow, T> rowExtractor) {
        return getDataCollection(file, (table, row) ->
                Optional.ofNullable(rowExtractor.apply(table, row))
                        .map(Collections::singletonList)
                        .orElse(emptyList()));
    }

    /**
     * Extracts objects from excel table without duplicate objects handling (duplicated row are both will be returned)
     */
    public <T> List<T> getDataCollection(Path file, BiFunction<? super Table, TableRow, Collection<T>> rowExtractor) {
        return getDataCollection(file, rowExtractor, Object::equals, (older, newer) -> Arrays.asList(older, newer));
    }

    /**
     * Extracts objects from excel table with duplicate objects handling logic
     */
    public <T> List<T> getDataCollection(Path file, BiFunction<? super Table, TableRow, Collection<T>> rowExtractor,
                                         BiPredicate<T, T> equalityChecker,
                                         BiFunction<T, T, Collection<T>> mergeDuplicates) {
        List<T> data = new ArrayList<>();
        for (TableRow row : this) {
            if (row != null) {
                try {
                    Collection<T> result = rowExtractor.apply(this, row);
                    if (result != null) {
                        for (T r : result) {
                            addWithEqualityChecker(r, data, equalityChecker, mergeDuplicates);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Не могу распарсить таблицу '{}' в файле {}, строка {}", tableName, file.getFileName(), row.getRowNum() + 1, e);
                }
            }
        }
        return data;
    }

    public static <T> void addWithEqualityChecker(T element,
                                                  Collection<T> collection,
                                                  BiPredicate<T, T> equalityChecker,
                                                  BiFunction<T, T, Collection<T>> mergeDuplicates) {
        T equalsObject = null;
        for (T e : collection) {
            if (equalityChecker.test(e, element)) {
                equalsObject = e;
                break;
            }
        }
        if (equalsObject != null) {
            collection.remove(equalsObject);
            collection.addAll(mergeDuplicates.apply(equalsObject, element));
        } else {
            collection.add(element);
        }
    }

    public int getIntCellValueOrDefault(TableRow row, TableColumnDescription columnDescription, int defaultValue) {
        try {
            return getIntCellValue(row, columnDescription);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public long getLongCellValueOrDefault(TableRow row, TableColumnDescription columnDescription, long defaultValue) {
        try {
            return getLongCellValue(row, columnDescription);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public BigDecimal getCurrencyCellValueOrDefault(TableRow row, TableColumnDescription columnDescription, BigDecimal defaultValue) {
        try {
            return getCurrencyCellValue(row, columnDescription);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String getStringCellValueOrDefault(TableRow row, TableColumnDescription columnDescription, String defaultValue) {
        try {
            return getStringCellValue(row, columnDescription);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public Iterator<TableRow> iterator() {
        return new TableIterator();
    }

    protected class TableIterator implements Iterator<TableRow> {
        private final int dataRowsCount = tableRange.getLastRow() - tableRange.getFirstRow()
                - dataRowOffset
                + (isLastTableRowContainsTotalData ? 0 : 1);
        private int cnt = 0;

        @Override
        public boolean hasNext() {
            return cnt < dataRowsCount;
        }

        @Override
        public TableRow next() {
            TableRow row;
            do {
                row = reportPage.getRow(tableRange.getFirstRow() + dataRowOffset + (cnt++));
            } while (row == null && hasNext());
            return row;
        }
    }

    @Override
    public TableRow findRow(Object value) {
        TableCellAddress address = reportPage.find(value);
        if (address.equals(TableCellAddress.NOT_FOUND)) {
            return null;
        }
        return reportPage.getRow(address.getRow());
    }
}
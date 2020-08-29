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

import ru.investbook.parser.TableColumnDescription;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public interface Table extends Iterable<TableRow> {

    <T> List<T> getData(Path file, BiFunction<? super Table, TableRow, T> rowExtractor);

    <T> List<T> getDataCollection(Path file, BiFunction<? super Table, TableRow, Collection<T>> rowExtractor);

    <T> List<T> getDataCollection(Path file, BiFunction<? super Table, TableRow, Collection<T>> rowExtractor,
                                  BiPredicate<T, T> equalityChecker,
                                  BiFunction<T, T, Collection<T>> mergeDuplicates);

    <T> void addWithEqualityChecker(T element,
                                    Collection<T> collection,
                                    BiPredicate<T, T> equalityChecker,
                                    BiFunction<T, T, Collection<T>> mergeDuplicates);

    /**
     * @return row containing given value or null if not found
     */
    TableRow findRow(Object value);

    Object getCellValue(TableRow row, TableColumnDescription columnDescription);

    /**
     * @throws RuntimeException if can't extract int value
     */
    int getIntCellValue(TableRow row, TableColumnDescription columnDescription);

    /**
     * @throws RuntimeException if can't extract long value
     */
    long getLongCellValue(TableRow row, TableColumnDescription columnDescription);

    /**
     * @throws RuntimeException if can't extract BigDecimal value
     */
    BigDecimal getCurrencyCellValue(TableRow row, TableColumnDescription columnDescription);

    /**
     * @throws RuntimeException if can't extract string value
     */
    String getStringCellValue(TableRow row, TableColumnDescription columnDescription);

    /**
     * @return return cell value or defaultValue if the cell is missing or the type does not match the expected
     */
    int getIntCellValueOrDefault(TableRow row, TableColumnDescription columnDescription, int defaultValue);

    /**
     * @return return cell value or defaultValue if the cell is missing or the type does not match the expected
     */
    long getLongCellValueOrDefault(TableRow row, TableColumnDescription columnDescription, long defaultValue);

    /**
     * @return return cell value or defaultValue if the cell is missing or the type does not match the expected
     */
    BigDecimal getCurrencyCellValueOrDefault(TableRow row, TableColumnDescription columnDescription, BigDecimal defaultValue);

    /**
     * @return return cell value or defaultValue if the cell is missing or the type does not match the expected
     */
    String getStringCellValueOrDefault(TableRow row, TableColumnDescription columnDescription, String defaultValue);
}

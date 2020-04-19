/*
 * Portfolio
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

package ru.portfolio.portfolio.parser;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;

/**
 * Implements table header kind of
 * <pre>
 * |             One             |             Two            |
 * |   a1    |   a2    |   a3    |   a1   |   a2    |   a3    |
 * | b1 | b2 | b1 | b2 | b1 | b2 |b1 | b2 | b1 | b2 | b1 | b2 |
 * <pre/>
 * Can find index for (Two -> a3 -> b1) column
 */
@RequiredArgsConstructor
public class MultiLineTableColumn implements TableColumn {
    private final TableColumn[] rowDescriptiors;

    /**
     * @param rowDescriptiors each array element describes next rows column
     */
    public static MultiLineTableColumn of(TableColumn... rowDescriptiors) {
        return new MultiLineTableColumn(rowDescriptiors);
    }

    /**
     * @param headerRows header rows count should be equal to count of row descriptors
     */
    @Override
    public int getColumnIndex(int firstColumnForSearch, Row... headerRows) {
        if (headerRows.length != rowDescriptiors.length) {
            throw new RuntimeException("Внутренняя ошибка, в таблице ожидается " + rowDescriptiors.length +
                    " строк в заголовке");
        }
        int columnIndex = firstColumnForSearch;
        int i = 0;
        for (Row row : headerRows) {
            TableColumn rowDescriptior = rowDescriptiors[i++];
            columnIndex = rowDescriptior.getColumnIndex(columnIndex, row);
        }
        return columnIndex;
    }
}

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

package ru.investbook.parser;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.poi.ss.usermodel.Row;

import java.util.Arrays;

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class AnyOfTableColumn implements TableColumn {

    private final TableColumn[] columns;

    public static TableColumn of(TableColumn... columns) {
        return new AnyOfTableColumn(columns);
    }

    @Override
    public int getColumnIndex(int firstColumnForSearch, Row... headerRows) {
        for (TableColumn c : columns) {
            try {
                return c.getColumnIndex(firstColumnForSearch, headerRows);
            } catch (RuntimeException ignore) {
            }
        }
        throw new RuntimeException("Не обнаружен заголовок таблицы, включающий: " + String.join(", ",
                Arrays.stream(columns)
                        .map(TableColumn::toString)
                        .toArray(String[]::new)));
    }
}

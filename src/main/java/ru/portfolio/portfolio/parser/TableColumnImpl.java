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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.util.Arrays;

@ToString
@EqualsAndHashCode
public class TableColumnImpl implements TableColumn {
    private final String[] words;

    public static TableColumn of(String... words) {
        return new TableColumnImpl(words);
    }

    private TableColumnImpl(String... words) {
        this.words = Arrays.stream(words)
                .map(String::toLowerCase)
                .toArray(String[]::new);
    }

    public int getColumnIndex(Row... headerRows) {
        for (Row header : headerRows) {
            next_cell:
            for (Cell cell : header) {
                if (cell.getCellType() == CellType.STRING) {
                    String colName = cell.getStringCellValue();
                    if (colName != null) {
                        colName = colName.toLowerCase();
                        for (String word : words) {
                            if (!containsWord(colName, word)) {
                                continue next_cell;
                            }
                        }
                        return cell.getColumnIndex();
                    }
                }
            }
        }
        throw new RuntimeException("Не обнаружен заголовок таблицы, включающий слова: " + String.join(", ", words));
    }

    private boolean containsWord(String text, String word) {
        return text.matches("(^|.*\\b|.*\\s)" + word + "(\\b.*|\\s.*|$)");
    }
}

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

package ru.investbook.parser.table.xml;

import lombok.RequiredArgsConstructor;
import nl.fountain.xelem.excel.Cell;
import org.spacious_team.table_wrapper.api.TableCell;

@RequiredArgsConstructor
public class XmlTableCell implements TableCell {

    private final Cell cell;

    @Override
    public int getColumnIndex() {
        return cell.getIndex() - 1;
    }

    @Override
    public Object getValue() {
        return cell.getData();
    }

    @Override
    public String getStringCellValue() {
        return cell.getData$();
    }
}

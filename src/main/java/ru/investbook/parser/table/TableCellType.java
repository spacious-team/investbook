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

import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.usermodel.FormulaError;

public enum TableCellType {
    /**
     * Unknown type, used to represent a state prior to initialization or the
     * lack of a concrete type.
     * For internal use only.
     */
    _NONE,

    /**
     * Numeric cell type (whole numbers, fractional numbers, dates)
     */
    NUMERIC,

    /** String (text) cell type */
    STRING,

    /**
     * Formula cell type
     * @see FormulaType
     */
    FORMULA,

    /**
     * Blank cell type
     */
    BLANK,

    /**
     * Boolean cell type
     */
    BOOLEAN,

    /**
     * Error cell type
     * @see FormulaError
     */
    ERROR;
}

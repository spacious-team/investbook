/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.web.model;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

@Data
public class ViewFilterModel {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate = LocalDate.of(1997, 9, 22);

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate = LocalDate.now();

    private Set<String> portfolios = Collections.emptySet();

    private boolean showDetails = true;
}

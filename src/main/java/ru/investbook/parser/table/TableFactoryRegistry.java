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

import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.api.TableFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

@Component
public class TableFactoryRegistry {

    private static final Collection<TableFactory> factories = new ArrayList<>();

    public TableFactoryRegistry(Collection<TableFactory> factories) {
        TableFactoryRegistry.factories.addAll(factories);
    }

    public static TableFactory get(ReportPage reportPage) {
        for (TableFactory factory : factories) {
            if (factory.canHandle(reportPage)) {
                return factory;
            }
        }
        throw new IllegalArgumentException("Нет парсера для отчета формата " + reportPage.getClass().getSimpleName());
    }
}

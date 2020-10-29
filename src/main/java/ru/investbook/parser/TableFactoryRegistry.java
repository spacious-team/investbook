/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser;

import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.api.TableFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TableFactoryRegistry {

    private static final Collection<TableFactory> factories = new ArrayList<>();

    public TableFactoryRegistry(Collection<TableFactory> factories) {
        log.info("Scanning implementations of {} ", TableFactory.class);
        long t0 = System.nanoTime();
        TableFactoryRegistry.factories.addAll(factories);
        findTableFactories();
        log.info("{} implementations of table factories found in {}: {}",
                factories.size(),
                Duration.ofNanos(System.nanoTime() - t0),
                factories.stream()
                        .map(TableFactory::getClass)
                        .collect(Collectors.toList()));
    }

    public static TableFactory get(ReportPage reportPage) {
        for (TableFactory factory : factories) {
            if (factory.canHandle(reportPage)) {
                return factory;
            }
        }
        throw new IllegalArgumentException("Нет парсера для отчета формата " + reportPage.getClass().getSimpleName());
    }

    private void findTableFactories() {
        String tableWrapperApiPackage = TableFactory.class.getPackage().getName();
        String tableWrapperPackage = tableWrapperApiPackage.substring(0, tableWrapperApiPackage.lastIndexOf('.'));
        Reflections reflections = new Reflections(tableWrapperPackage);
        reflections.getSubTypesOf(TableFactory.class)
                .stream()
                .map(TableFactoryRegistry::getTableFactoryConstructor)
                .filter(Objects::nonNull)
                .map(TableFactoryRegistry::getTableFactoryInstance)
                .filter(Objects::nonNull)
                .forEach(factories::add);
    }

    private static Constructor<? extends TableFactory> getTableFactoryConstructor(Class<? extends TableFactory> clazz) {
        try {
            return clazz.getConstructor();
        } catch (Exception e) {
            log.error("Can't find default public constructor for table factory class " + clazz, e);
            return null;
        }
    }

    private static TableFactory getTableFactoryInstance(Constructor<? extends TableFactory> constructor) {
        try {
            return constructor.newInstance();
        } catch (Exception e) {
            log.trace("Can't create instance of " + constructor, e);
            return null;
        }
    }
}

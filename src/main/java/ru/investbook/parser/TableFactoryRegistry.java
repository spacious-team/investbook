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
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.api.TableFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import ru.investbook.InvestbookProperties;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.System.nanoTime;

@Component
@Slf4j
public class TableFactoryRegistry {

    private static final Collection<TableFactory> factories = new ArrayList<>();

    public TableFactoryRegistry(Collection<TableFactory> tableFactories, InvestbookProperties properties) {
        long t0 = nanoTime();
        Collection<String> tableFactoryPackages = new HashSet<>();
        tableFactoryPackages.add(getDefaultTableFactoryPackage());
        tableFactoryPackages.addAll(properties.getTableParsers());
        tableFactoryPackages.forEach(TableFactoryRegistry::addTableFactories);
        factories.addAll(tableFactories);
        log.info("{} implementations of table factory found in {}: {}", factories.size(), Duration.ofNanos(nanoTime() - t0),
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

    private static String getDefaultTableFactoryPackage() {
        String tableWrapperApiPackage = TableFactory.class.getPackage().getName();
        return tableWrapperApiPackage.substring(0, tableWrapperApiPackage.lastIndexOf('.'));
    }

    private static void addTableFactories(String basePackage) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(TableFactory.class));
        scanner.findCandidateComponents(basePackage)
                .stream()
                .map(BeanDefinition::getBeanClassName)
                .map(TableFactoryRegistry::getInstance)
                .filter(Objects::nonNull)
                .forEach(factories::add);
    }

    private static TableFactory getInstance(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor();
            return (TableFactory) constructor.newInstance();
        } catch (Exception e) {
            log.trace("Can't create instance of " + className, e);
            return null;
        }
    }
}

/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.parser.investbook;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractBrokerReportFactory;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@SuppressWarnings("DefaultAnnotationParam")
public class InvestbookBrokerReportFactory extends AbstractBrokerReportFactory {
    @Getter
    private final String brokerName = "Investbook";

    @Override
    public boolean canCreate(String fileName, InputStream is) {
        return fileName.endsWith(".csv") || fileName.endsWith(".xls") || fileName.endsWith(".xlsx");
    }

    @Override
    public Optional<BrokerReport> create(String fileName, InputStream is) {
        Optional<BrokerReport> brokerReport = create(fileName, is, InvestbookBrokerReport::new);
        if (brokerReport.isPresent()) {
            log.info("Обнаружен отчет '{}' в формате Investbook", fileName);
        }
        return brokerReport;
    }
}

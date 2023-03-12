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

package ru.investbook.parser.sber.transaction;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractBrokerReportFactory;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.investbook.parser.SecurityRegistrar;

import java.io.InputStream;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@Order(PriorityOrdered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class SberTrBrokerReportFactory extends AbstractBrokerReportFactory {
    private final SecurityRegistrar securityRegistrar;

    @Getter
    private final String brokerName = "Сбербанк Онлайн (сделки)";
    private final Pattern expectedFileNamePattern = Pattern.compile("^Сделки[_-].*");

    @Override
    public boolean canCreate(String excelFileName, InputStream is) {
        return super.canCreate(expectedFileNamePattern, excelFileName, is);
    }

    @Override
    public Optional<BrokerReport> create(String excelFileName, InputStream is) {
        Optional<BrokerReport> brokerReport = create(excelFileName, is,
                (fileName, stream) -> new SberTrBrokerReport(fileName, stream, securityRegistrar));
        if (brokerReport.isPresent()) {
            log.info("Обнаружен отчет сделок '{}' Сбербанк брокера", excelFileName);
        }
        return brokerReport;
    }
}

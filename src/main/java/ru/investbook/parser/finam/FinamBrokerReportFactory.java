/*
 * InvestBook
 * Copyright (C) 2023  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.parser.finam;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractBrokerReportFactory;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.investbook.parser.SecurityRegistrar;

import java.io.InputStream;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // TODO:why???
@Slf4j
@RequiredArgsConstructor
public class FinamBrokerReportFactory extends AbstractBrokerReportFactory {

    private final SecurityRegistrar securityRegistrar;

    @Getter
    private final String brokerName = "ФИНАМ";

    private final Pattern expectedFileNamePattern = Pattern.compile(
            "^(\\p{L}+_){3}КлФ_[0-9]+_\\d{2}_\\d{2}_\\d{4}_по_\\d{2}_\\d{2}_\\d{4}\\.xls(x)?$"
    );

    @Override
    public boolean canCreate(String excelFileName, InputStream is) {
        return super.canCreate(expectedFileNamePattern, excelFileName, is);
    }

    @Override
    public Optional<BrokerReport> create(String excelFileName, InputStream is) {
        Optional<BrokerReport> brokerReport = create(excelFileName, is,
                (fileName, stream) -> new FinamBrokerReport(fileName, stream, securityRegistrar));
        if (brokerReport.isPresent()) {
            log.info("Обнаружен отчет '{}' Финам брокера", excelFileName);
        }
        return brokerReport;
    }
}

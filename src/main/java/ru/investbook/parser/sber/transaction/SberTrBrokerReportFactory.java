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

package ru.investbook.parser.sber.transaction;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractBrokerReportFactory;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.regex.Pattern;

@Component
@Slf4j
public class SberTrBrokerReportFactory extends AbstractBrokerReportFactory {

    @Getter
    private final String brokerName = "Сбербанк Онлайн (сделки)";
    private final Pattern expectedFileNamePattern = Pattern.compile("^Сделки_.*");

    @Override
    public BrokerReport create(String excelFileName, InputStream is) {
        BrokerReport brokerReport = create(expectedFileNamePattern, excelFileName, is, SberTrBrokerReport::new);
        if (brokerReport != null) {
            log.info("Обнаружен отчет сделок '{}' Сбербанк брокера", excelFileName);
        }
        return brokerReport;
    }
}

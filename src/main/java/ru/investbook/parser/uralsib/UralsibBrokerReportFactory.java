/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractBrokerReportFactory;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

@Component
@Slf4j
public class UralsibBrokerReportFactory extends AbstractBrokerReportFactory {

    @Getter
    private final String brokerName = "Уралсиб";
    private final Pattern zippedExpectedFileNamePattern = Pattern.compile("^brok_rpt_.*\\.xls(x)?\\.zip$");
    private final Pattern expectedFileNamePattern = Pattern.compile("^brok_rpt_.*\\.xls(x)?$");

    @Override
    public BrokerReport create(String excelFileName, InputStream is) {
        BrokerReport brokerReport;
        if (excelFileName.toLowerCase().endsWith(".zip")) {
            brokerReport = create(
                    zippedExpectedFileNamePattern,
                    excelFileName,
                    is,
                    (fileName, istream) -> new UralsibBrokerReport(new ZipInputStream(istream)));
        } else {
            brokerReport = create(
                    expectedFileNamePattern,
                    excelFileName,
                    is,
                    UralsibBrokerReport::new);
        }
        if (brokerReport != null) {
            log.info("Обнаружен отчет '{}' Уралсиб брокера", excelFileName);
            if (!excelFileName.contains("_invest_")) {
                log.warn("Рекомендуется загружать отчеты Уралсиб брокера, содержащие в имени файла слово 'invest'");
            }
        }
        return brokerReport;
    }
}

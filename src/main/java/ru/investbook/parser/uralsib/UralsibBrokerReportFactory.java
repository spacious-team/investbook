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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractBrokerReportFactory;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.investbook.parser.SecurityRegistrar;

import java.io.InputStream;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

@Component
@Order(PriorityOrdered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class UralsibBrokerReportFactory extends AbstractBrokerReportFactory {
    private final SecurityRegistrar securityRegistrar;

    @Getter
    private final String brokerName = "Уралсиб";
    private final Pattern zippedExpectedFileNamePattern = Pattern.compile("^brok_rpt_.*\\.xls(x)?\\.zip$");
    private final Pattern expectedFileNamePattern = Pattern.compile("^brok_rpt_.*\\.xls(x)?$");

    @Override
    public boolean canCreate(String excelFileName, InputStream is) {
        return excelFileName.toLowerCase().endsWith(".zip") ?
                super.canCreate(zippedExpectedFileNamePattern, excelFileName, is) :
                super.canCreate(expectedFileNamePattern, excelFileName, is);
    }

    @Override
    public BrokerReport create(String excelFileName, InputStream is) {
        BrokerReport brokerReport;
        BiFunction<String, InputStream, BrokerReport> reportProvider;
        if (excelFileName.toLowerCase().endsWith(".zip")) {
            reportProvider = (fileName, stream) -> new UralsibBrokerReport(new ZipInputStream(stream), securityRegistrar);
        } else {
            reportProvider = (fileName, stream) ->  new UralsibBrokerReport(fileName, stream, securityRegistrar);
        }
        brokerReport = create(excelFileName, is, reportProvider);
        if (brokerReport != null) {
            log.info("Обнаружен отчет '{}' Уралсиб брокера", excelFileName);
            if (!excelFileName.contains("_invest_")) {
                log.warn("Рекомендуется загружать отчеты Уралсиб брокера, содержащие в имени файла слово 'invest'");
            }
        }
        return brokerReport;
    }
}

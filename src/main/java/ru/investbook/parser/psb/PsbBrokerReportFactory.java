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

package ru.investbook.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.investbook.parser.AbstractBrokerReportFactory;
import ru.investbook.parser.BrokerReport;

import java.io.InputStream;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PsbBrokerReportFactory extends AbstractBrokerReportFactory {

    @Getter
    private final String brokerName = "Промсвязьбанк";
    private final Pattern expectedFileNamePattern = Pattern.compile("^[0-9]+\\([^()]+\\)[0-9\\-]+\\.xls(x)?$");

    @Override
    public BrokerReport create(String excelFileName, InputStream is) {
        BrokerReport brokerReport = create(expectedFileNamePattern, excelFileName, is, PsbBrokerReport::new);
        if (brokerReport != null) {
            log.info("Обнаружен отчет '{}' фондового и срочного рынков Промсвязьбанк брокера", excelFileName);
        }
        return brokerReport;
    }
}

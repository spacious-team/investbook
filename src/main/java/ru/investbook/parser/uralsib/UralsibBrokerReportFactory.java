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

package ru.investbook.parser.uralsib;

import org.springframework.stereotype.Component;
import ru.investbook.parser.AbstractBrokerReportFactory;
import ru.investbook.parser.BrokerReport;

import java.io.InputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

@Component
public class UralsibBrokerReportFactory extends AbstractBrokerReportFactory {

    private final Pattern zippedExpectedFileNamePattern = Pattern.compile("^brok_rpt_.*\\.xls(x)?\\.zip$");
    private final Pattern expectedFileNamePattern = Pattern.compile("^brok_rpt_.*\\.xls(x)?$");

    @Override
    public BrokerReport create(String excelFileName, InputStream is) {
        if (excelFileName.toLowerCase().endsWith(".zip")) {
            return create(
                    zippedExpectedFileNamePattern,
                    excelFileName,
                    is,
                    (fileName, istream) -> new UralsibBrokerReport(new ZipInputStream(istream)));
        } else {
            return create(
                    expectedFileNamePattern,
                    excelFileName,
                    is,
                    UralsibBrokerReport::new);
        }
    }
}

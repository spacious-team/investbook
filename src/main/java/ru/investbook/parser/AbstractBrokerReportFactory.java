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

package ru.investbook.parser;

import org.apache.poi.util.CloseIgnoringInputStream;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public abstract class AbstractBrokerReportFactory implements BrokerReportFactory {

    /**
     * Checks input stream and returns broker report if can, otherwise reset input stream mark to original position
     * and returns null
     *
     * @param expectedFileNamePattern used for fast report check without input stream reading
     * @return broker report if can parse or null
     * @throws IllegalArgumentException if InputStream is not supports mark
     */
    public BrokerReport create(Pattern expectedFileNamePattern, String excelFileName, InputStream is,
                               BiFunction<String, InputStream, BrokerReport> brokerReportProvider) {
        if (!expectedFileNamePattern.matcher(excelFileName).matches()) {
            return null;
        }
        Assert.isTrue(is.markSupported(), "Provided input stream doesn't supports mark");
        is = new CloseIgnoringInputStream(is); // do not close stream
        is.mark(1024);
        Exception exception = null;
        try {
            return brokerReportProvider.apply(excelFileName, is);
        } catch (Exception e) {
            exception = e;
            return null;
        } finally {
            resetInputStream(is, exception);
        }
    }

    private static void resetInputStream(InputStream is, Throwable t) {
        try {
            is.reset();
        } catch (IOException ioe) {
            if (t != null) {
                ioe.addSuppressed(t);
            }
            throw new RuntimeException("Can't reset input stream", ioe);
        }
    }
}

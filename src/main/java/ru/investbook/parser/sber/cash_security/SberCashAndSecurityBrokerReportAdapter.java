/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser.sber.cash_security;

import lombok.Getter;
import org.apache.poi.ss.usermodel.Workbook;
import org.spacious_team.table_wrapper.api.ReportPage;
import ru.investbook.parser.MultiPortfolioBrokerReport;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static ru.investbook.parser.AbstractExcelBrokerReport.getWorkBook;
import static ru.investbook.parser.sber.SberBrokerReportHelper.findPortfolios;

public class SberCashAndSecurityBrokerReportAdapter implements MultiPortfolioBrokerReport {

    @Getter
    private final SberCashBrokerReport cashReport;

    @Getter
    private final SberSecurityDepositBrokerReport securityDepositReport;

    @Getter
    private final Set<String> portfolios;

    private final Workbook book;

    public SberCashAndSecurityBrokerReportAdapter(String excelFileName, InputStream is) {
        this.book = getWorkBook(excelFileName, is);
        this.cashReport = new SberCashBrokerReport(excelFileName, book);
        this.securityDepositReport = new SberSecurityDepositBrokerReport(excelFileName, book);
        Set<String> portfolios = new HashSet<>(findPortfolios(cashReport.getReportPage()));
        portfolios.addAll(findPortfolios(securityDepositReport.getReportPage()));
        this.portfolios = Set.copyOf(portfolios);
    }

    @Override
    public ReportPage getReportPage() {
        throw new UnsupportedOperationException("This is a BrokerReport adapter");
    }

    @Override
    public void close() throws IOException {
        book.close();
    }
}

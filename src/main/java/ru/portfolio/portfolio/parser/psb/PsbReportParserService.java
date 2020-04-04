/*
 * Portfolio
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

package ru.portfolio.portfolio.parser.psb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.portfolio.portfolio.parser.ReportTable;
import ru.portfolio.portfolio.parser.ReportTableSaver;
import ru.portfolio.portfolio.pojo.EventCashFlow;
import ru.portfolio.portfolio.pojo.Portfolio;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class PsbReportParserService {
    private final ReportTableSaver saver;


    public void parse(String reportFile) {
        parse(Paths.get(reportFile));
    }

    public void parse(Path reportFile) {
        try (PsbBrokerReport report = new PsbBrokerReport(reportFile)) {
            boolean isAdded = saver.addPortfolio(Portfolio.builder().id(report.getPortfolio()).build());
            if (isAdded) {
                CashTable cashTable = new CashTable(report);
                PortfolioPropertyTable portfolioPropertyTable = new PortfolioPropertyTable(report);
                CashFlowTable cashFlowTable = new CashFlowTable(report);
                PortfolioSecuritiesTable portfolioSecuritiesTable = new PortfolioSecuritiesTable(report);
                TransactionTable transactionTable = new TransactionTable(report);
                CouponAndAmortizationTable couponAndAmortizationTable = new CouponAndAmortizationTable(report);
                DividendTable dividendTable = new DividendTable(report);
                DerivativeTransactionTable derivativeTransactionTable = new DerivativeTransactionTable(report);
                DerivativeCashFlowTable derivativeCashFlowTable = new DerivativeCashFlowTable(report);

                portfolioPropertyTable.getData().forEach(saver::addPortfolioProperty);
                saver.addCashInfo(cashTable);
                portfolioSecuritiesTable.getData().forEach(saver::addSecurity);
                cashFlowTable.getData().forEach(saver::addEventCashFlow);
                transactionTable.getData().forEach(saver::addTransaction);
                couponAndAmortizationTable.getData().forEach(c -> {
                    saver.addSecurity(c.getIsin()); // required for amortization
                    saver.addSecurityEventCashFlow(c);
                });
                dividendTable.getData().forEach(saver::addSecurityEventCashFlow);
                derivativeTransactionTable.getData().forEach(saver::addTransaction);
                addDerivativeCashFlows(derivativeCashFlowTable);
            }
        } catch (Exception e) {
            log.warn("Не могу открыть/закрыть отчет {}", reportFile, e);
            throw new RuntimeException(e);
        }
    }

    private void addDerivativeCashFlows(ReportTable<DerivativeCashFlowTable.DerivativeCashFlowTableRow> derivativeCashFlowTable) {
        for (DerivativeCashFlowTable.DerivativeCashFlowTableRow row : derivativeCashFlowTable.getData()) {
            if (row.getContract() != null) {
                boolean isAdded = saver.addSecurity(row.getContract());
                if (!isAdded) continue;
                saver.addSecurityEventCashFlow(SecurityEventCashFlow.builder()
                        .isin(row.getContract())
                        .portfolio(derivativeCashFlowTable.getReport().getPortfolio())
                        .count(row.getCount())
                        .eventType(row.getEvent())
                        .timestamp(row.getTimestamp())
                        .value(row.getValue())
                        .currency(row.getCurrency())
                        .build());
            } else {
                // Событие "Биржевой сбор"
                saver.addEventCashFlow(EventCashFlow.builder()
                        .portfolio(derivativeCashFlowTable.getReport().getPortfolio())
                        .eventType(row.getEvent())
                        .timestamp(row.getTimestamp())
                        .value(row.getValue())
                        .currency(row.getCurrency())
                        .build());
            }
        }
    }
}

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.portfolio.portfolio.parser.ReportTable;
import ru.portfolio.portfolio.parser.ReportTableSaver;
import ru.portfolio.portfolio.pojo.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.portfolio.portfolio.parser.psb.DerivativeTransactionTable.QUOTE_CURRENCY;

@Service
@Slf4j
@RequiredArgsConstructor
public class PsbReportParserService {
    private final ReportTableSaver saver;
    private final ObjectMapper objectMapper;


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
                addCashInfo(cashTable);
                portfolioSecuritiesTable.getData().forEach(saver::addSecurity);
                cashFlowTable.getData().forEach(saver::addEventCashFlow);
                addTransaction(transactionTable);
                couponAndAmortizationTable.getData().forEach(c -> {
                    saver.addSecurity(c.getIsin()); // required for amortization
                    saver.addSecurityEventCashFlow(c);
                });
                dividendTable.getData().forEach(saver::addSecurityEventCashFlow);
                addDerivativeTransaction(derivativeTransactionTable);
                addDerivativeCashFlows(derivativeCashFlowTable);
            }
        } catch (Exception e) {
            log.warn("Не могу открыть/закрыть отчет {}", reportFile, e);
            throw new RuntimeException(e);
        }
    }

    private void addTransaction(ReportTable<TransactionTable.SecurityTransaction> transactionTable) {
        for (TransactionTable.SecurityTransaction row : transactionTable.getData()) {
            boolean isAdded = saver.addTransaction(Transaction.builder()
                    .id(row.getTransactionId())
                    .portfolio(transactionTable.getReport().getPortfolio())
                    .isin(row.getIsin())
                    .timestamp(row.getTimestamp())
                    .count(row.getCount())
                    .build());
            if (isAdded) {
                TransactionCashFlow cashFlow = TransactionCashFlow.builder()
                        .transactionId(row.getTransactionId())
                        .build();
                if (!row.getValue().equals(BigDecimal.ZERO)) {
                    saver.addTransactionCashFlow(cashFlow.toBuilder()
                            .eventType(CashFlowType.PRICE)
                            .value(row.getValue())
                            .currency(row.getValueCurrency())
                            .build());
                }
                if (!row.getAccruedInterest().equals(BigDecimal.ZERO)) {
                    saver.addTransactionCashFlow(cashFlow.toBuilder()
                            .eventType(CashFlowType.ACCRUED_INTEREST)
                            .value(row.getAccruedInterest())
                            .currency(row.getValueCurrency())
                            .build());
                }
                if (!row.getCommission().equals(BigDecimal.ZERO)) {
                    saver.addTransactionCashFlow(cashFlow.toBuilder()
                            .eventType(CashFlowType.COMMISSION)
                            .value(row.getCommission())
                            .currency(row.getCommissionCurrency())
                            .build());
                }
            }
        }
    }

    private void addDerivativeTransaction(ReportTable<DerivativeTransactionTable.DerivativeTransaction> derivativeTransactionTable) {
        for (DerivativeTransactionTable.DerivativeTransaction row : derivativeTransactionTable.getData()) {
            saver.addSecurity(row.getContract());
            boolean isAdded = saver.addTransaction(Transaction.builder()
                    .id(row.getTransactionId())
                    .portfolio(derivativeTransactionTable.getReport().getPortfolio())
                    .isin(row.getContract())
                    .timestamp(row.getTimestamp())
                    .count(row.getCount())
                    .build());
            if (isAdded) {
                TransactionCashFlow cashFlow = TransactionCashFlow.builder()
                        .transactionId(row.getTransactionId())
                        .build();
                if (!row.getValue().equals(BigDecimal.ZERO)) {
                    saver.addTransactionCashFlow(cashFlow.toBuilder()
                            .eventType(row.getValueCurrency().equals(QUOTE_CURRENCY) ?
                                    CashFlowType.DERIVATIVE_QUOTE :
                                    CashFlowType.DERIVATIVE_PRICE)
                            .value(row.getValue())
                            .currency(row.getValueCurrency())
                            .build());
                }
                if (!row.getCommission().equals(BigDecimal.ZERO)) {
                    saver.addTransactionCashFlow(cashFlow.toBuilder()
                            .eventType(CashFlowType.COMMISSION)
                            .value(row.getCommission())
                            .currency(row.getCommissionCurrency())
                            .build());
                }
            }
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

    private void addCashInfo(ReportTable<CashTable.CashTableRow> cashTable) {
        try {
            saver.addPortfolioProperty(PortfolioProperty.builder()
                .portfolio(cashTable.getReport().getPortfolio())
                .property(PortfolioPropertyType.CASH)
                .value(objectMapper.writeValueAsString(cashTable.getData()))
                .timestamp(cashTable.getReport().getReportDate())
                .build());
        } catch (JsonProcessingException e) {
            log.warn("Не могу добавить информацию о наличных средствах {}", cashTable.getData(), e);
        }
    }
}

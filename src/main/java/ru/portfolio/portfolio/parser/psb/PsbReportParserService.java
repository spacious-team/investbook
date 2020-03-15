package ru.portfolio.portfolio.parser.psb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.portfolio.portfolio.controller.*;
import ru.portfolio.portfolio.pojo.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class PsbReportParserService {
    private final PortfolioRestController portfolioRestController;
    private final SecurityRestController securityRestController;
    private final SecurityEventCashFlowRestController securityEventCashFlowRestController;
    private final EventCashFlowRestController eventCashFlowRestController;
    private final TransactionRestController transactionRestController;
    private final TransactionCashFlowRestController transactionCashFlowRestController;

    public void parse(String reportFile) {
        parse(Paths.get(reportFile));
    }

    public void parse(Path reportFile) {
        try (PsbBrokerReport report = new PsbBrokerReport(reportFile)) {
            boolean isAdded = addPortfolio(Portfolio.builder().portfolio(report.getPortfolio()));
            if (isAdded) {
                //CashTable cashTable = new CashTable(report);
                CashFlowTable cashFlowTable = new CashFlowTable(report);
                PortfolioSecuritiesTable portfolioSecuritiesTable = new PortfolioSecuritiesTable(report);
                TransactionTable transactionTable = new TransactionTable(report);
                CouponAndAmortizationTable couponAndAmortizationTable = new CouponAndAmortizationTable(report);
                DividendTable dividendTable = new DividendTable(report);
                DerivativeTransactionTable derivativeTransactionTable = new DerivativeTransactionTable(report);
                DerivativeCashFlowTable derivativeCashFlowTable = new DerivativeCashFlowTable(report);

                addSecurities(portfolioSecuritiesTable);
                addCashInAndOutFlows(cashFlowTable);
                addTransaction(transactionTable);
                addCouponAndAmortizationCashFlows(couponAndAmortizationTable);
                addDividendCashFlows(dividendTable);
                addDerivativeTransaction(derivativeTransactionTable);
                addDerivativeCashFlows(derivativeCashFlowTable);
            }
        } catch (Exception e) {
            log.warn("Не могу открыть/закрыть отчет {}", reportFile, e);
            throw new RuntimeException(e);
        }
    }

    private void addSecurities(PortfolioSecuritiesTable portfolioSecuritiesTable) {
        for (PortfolioSecuritiesTable.PortfolioSecuritiesTableRow row : portfolioSecuritiesTable.getData()) {
            addSecurity(row.getIsin(), row.getName());
        }
    }

    private void addCashInAndOutFlows(CashFlowTable cashFlowTable) {
        for (CashFlowTable.CashFlowTableRow row : cashFlowTable.getData()) {
            addEventCashFlow(EventCashFlow.builder()
                    .portfolio(cashFlowTable.getReport().getPortfolio())
                    .eventType(row.getType())
                    .timestamp(row.getTimestamp())
                    .value(row.getValue())
                    .currency(row.getCurrency()));
        }
    }

    private void addTransaction(TransactionTable transactionTable) {
        for (TransactionTable.Row row : transactionTable.getData()) {
            try {
                boolean isAdded = addTransaction(Transaction.builder()
                        .id(row.getTransactionId())
                        .portfolio(transactionTable.getReport().getPortfolio())
                        .isin(row.getIsin())
                        .timestamp(row.getTimestamp())
                        .count(row.getCount()));
                if (isAdded) {
                    TransactionCashFlow cashFlow = TransactionCashFlow.builder()
                            .transactionId(row.getTransactionId())
                            .currency(row.getCurrency())
                            .build();
                    if (!row.getValue().equals(BigDecimal.ZERO)) {
                        addTransactionCashFlow(cashFlow.toBuilder()
                                .eventType(CashFlowType.PRICE)
                                .value(row.getValue()));
                    }
                    if (!row.getAccruedInterest().equals(BigDecimal.ZERO)) {
                        addTransactionCashFlow(cashFlow.toBuilder()
                                .eventType(CashFlowType.ACCRUED_INTEREST)
                                .value(row.getAccruedInterest()));
                    }
                    if (!row.getCommission().equals(BigDecimal.ZERO)) {
                        addTransactionCashFlow(cashFlow.toBuilder()
                                .eventType(CashFlowType.COMMISSION)
                                .value(row.getCommission()));
                    }
                }
            } catch (Exception e) {
                log.warn("Не могу добавить транзакцию {}", row, e);
            }
        }
    }

    private void addCouponAndAmortizationCashFlows(CouponAndAmortizationTable couponAndAmortizationTable) {
        for (CouponAndAmortizationTable.CouponAndAmortizationTableRow row : couponAndAmortizationTable.getData()) {
            addSecurity(row.getIsin()); // required for amortization
            addSecurityEventCashFlow(SecurityEventCashFlow.builder()
                    .isin(row.getIsin())
                    .portfolio(couponAndAmortizationTable.getReport().getPortfolio())
                    .count(row.getCount())
                    .eventType(row.getEvent())
                    .timestamp(row.getTimestamp())
                    .value(row.getValue())
                    .currency(row.getCurrency()));
        }
    }

    private void addDividendCashFlows(DividendTable dividendTable) {
        for (DividendTable.DividendTableRow row : dividendTable.getData()) {
            addSecurityEventCashFlow(SecurityEventCashFlow.builder()
                    .isin(row.getIsin())
                    .portfolio(dividendTable.getReport().getPortfolio())
                    .count(row.getCount())
                    .eventType(row.getEvent())
                    .timestamp(row.getTimestamp())
                    .value(row.getValue())
                    .currency(row.getCurrency()));
        }
    }

    private void addDerivativeTransaction(DerivativeTransactionTable derivativeTransactionTable) {
        for (DerivativeTransactionTable.FortsTableRow row : derivativeTransactionTable.getData()) {
            addSecurity(row.getIsin());
            try {
                boolean isAdded = addTransaction(Transaction.builder()
                        .id(row.getTransactionId())
                        .portfolio(derivativeTransactionTable.getReport().getPortfolio())
                        .isin(row.getIsin())
                        .timestamp(row.getTimestamp())
                        .count(row.getCount()));
                if (isAdded) {
                    TransactionCashFlow cashFlow = TransactionCashFlow.builder()
                            .transactionId(row.getTransactionId())
                            .currency(row.getCurrency())
                            .build();
                    if (!row.getValue().equals(BigDecimal.ZERO)) {
                        addTransactionCashFlow(cashFlow.toBuilder()
                                .eventType(CashFlowType.DERIVATIVE_PRICE)
                                .value(row.getValue()));
                    }
                    if (!row.getCommission().equals(BigDecimal.ZERO)) {
                        addTransactionCashFlow(cashFlow.toBuilder()
                                .eventType(CashFlowType.COMMISSION)
                                .value(row.getCommission()));
                    }
                }
            } catch (Exception e) {
                log.warn("Не могу добавить транзакцию {}", row, e);
            }
        }
    }

    private void addDerivativeCashFlows(DerivativeCashFlowTable derivativeCashFlowTable) {
        for (DerivativeCashFlowTable.DerivativeCashFlowTableRow row : derivativeCashFlowTable.getData()) {
            if (row.getContract() != null) {
                boolean isAdded = addSecurity(row.getContract());
                if (!isAdded) continue;
                addSecurityEventCashFlow(SecurityEventCashFlow.builder()
                        .isin(row.getContract())
                        .portfolio(derivativeCashFlowTable.getReport().getPortfolio())
                        .count(row.getCount())
                        .eventType(row.getEvent())
                        .timestamp(row.getTimestamp())
                        .value(row.getValue())
                        .currency(row.getCurrency()));
            } else {
                // Событие "Биржевой сбор"
                addEventCashFlow(EventCashFlow.builder()
                        .portfolio(derivativeCashFlowTable.getReport().getPortfolio())
                        .eventType(row.getEvent())
                        .timestamp(row.getTimestamp())
                        .value(row.getValue())
                        .currency(row.getCurrency()));
            }
        }
    }

    private boolean addSecurity(String isin) {
        return addSecurity(isin, null);
    }

    private boolean addSecurity(String isin, String name) {
        return addSecurity(Security.builder()
                .isin(isin)
                .name(name));
    }

    private boolean addPortfolio(Portfolio.PortfolioBuilder portfolio) {
        try {
            HttpStatus status = portfolioRestController.post(portfolio.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу сохранить Портфель {}", portfolio);
                return false;
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу сохранить Портфель {}", portfolio, e);
            } else {
                log.warn("Не могу сохранить Портфель {}", portfolio, e);
            }
            return false;
        }
        return true;
    }

    private boolean addSecurity(Security.SecurityBuilder security) {
        try {
            HttpStatus status = securityRestController.post(security.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить ЦБ {} в список", security);
                return false;
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу добавить ЦБ {} в список", security, e);
            } else {
                log.warn("Не могу добавить ЦБ {} в список", security, e);
            }
            return false;
        }
        return true;
    }

    private boolean addTransaction(Transaction.TransactionBuilder transaction) {
        try {
            HttpStatus status = transactionRestController.post(transaction.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить транзакцию {}", transaction);
                return false;
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу добавить транзакцию {}", transaction, e);
            } else {
                log.warn("Не могу добавить транзакцию {}", transaction, e);
            }
            return false;
        }
        return true;
    }

    private void addTransactionCashFlow(TransactionCashFlow.TransactionCashFlowBuilder transactionCashFlow) {
        try {
            HttpStatus status = transactionCashFlowRestController.post(transactionCashFlow.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить информацию о передвижении средств {}", transactionCashFlow);
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу добавить информацию о передвижении средств {}", transactionCashFlow, e);
            } else {
                log.warn("Не могу добавить информацию о передвижении средств {}", transactionCashFlow, e);
            }
        }
    }

    private void addEventCashFlow(EventCashFlow.EventCashFlowBuilder eventCashFlow) {
        try {
            HttpStatus status = eventCashFlowRestController.post(eventCashFlow.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить информацию о движении денежных средств {}", eventCashFlow);
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу добавить информацию о движении денежных средств {}", eventCashFlow, e);
            } else {
                log.warn("Не могу добавить информацию о движении денежных средств {}", eventCashFlow, e);
            }
        }
    }

    private void addSecurityEventCashFlow(SecurityEventCashFlow.SecurityEventCashFlowBuilder securityEventCashFlow) {
        try {
            HttpStatus status = securityEventCashFlowRestController.post(securityEventCashFlow.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить информацию о движении денежных средств {}", securityEventCashFlow);
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу добавить информацию о движении денежных средств {}", securityEventCashFlow, e);
            } else {
                log.warn("Не могу добавить информацию о движении денежных средств {}", securityEventCashFlow, e);
            }
        }
    }
}

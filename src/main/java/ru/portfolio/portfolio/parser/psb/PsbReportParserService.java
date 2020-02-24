package ru.portfolio.portfolio.parser.psb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.portfolio.portfolio.controller.EventCashFlowRestController;
import ru.portfolio.portfolio.controller.SecurityRestController;
import ru.portfolio.portfolio.controller.TransactionCashFlowRestController;
import ru.portfolio.portfolio.controller.TransactionRestController;
import ru.portfolio.portfolio.pojo.*;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class PsbReportParserService {
    private final SecurityRestController securityRestController;
    private final EventCashFlowRestController eventCashFlowRestController;
    private final TransactionRestController transactionRestController;
    private final TransactionCashFlowRestController transactionCashFlowRestController;

    public void parse(String reportFile) {
        try (PsbBrokerReport report = new PsbBrokerReport(reportFile)) {
            //CashTable cashTable = new CashTable(report);
            CashFlowTable cashFlowTable = new CashFlowTable(report);
            PortfolioTable portfolioTable = new PortfolioTable(report);
            TransactionTable transactionTable = new TransactionTable(report);
            CouponAndAmortizationTable couponAndAmortizationTable = new CouponAndAmortizationTable(report);
            DividendTable dividendTable = new DividendTable(report);
            DerivativeTransactionTable derivativeTransactionTable = new DerivativeTransactionTable(report);

            addSecurities(portfolioTable);
            addCashInAndOutFlows(cashFlowTable);
            addTransaction(transactionTable);
            addCouponAndAmortizationCashFlows(couponAndAmortizationTable);
            addDividendCashFlows(dividendTable);
            addDerivativeTransaction(derivativeTransactionTable);
        } catch (Exception e) {
            log.warn("Не могу открыть/закрыть отчет {}", reportFile, e);
        }
    }

    private void addSecurities(PortfolioTable portfolioTable) {
        for (PortfolioTable.Row row : portfolioTable.getData()) {
            try {
                Security security = Security.builder()
                        .isin(row.getIsin())
                        .name(row.getName())
                        .build();
                HttpStatus status = securityRestController.post(security).getStatusCode();
                if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                    log.warn("Не могу добавить ЦБ {} в список", row);
                }
            } catch (Exception e) {
                log.warn("Не могу добавить ЦБ {} в список", row, e);
            }
        }
    }

    private void addCashInAndOutFlows(CashFlowTable cashFlowTable) {
        for (CashFlowTable.Row row : cashFlowTable.getData()) {
            try {
                EventCashFlow eventCashFlow = EventCashFlow.builder()
                        .eventType(row.getType())
                        .timestamp(row.getTimestamp())
                        .value(row.getValue())
                        .currency(row.getCurrency())
                        .build();
                HttpStatus status = eventCashFlowRestController.post(eventCashFlow).getStatusCode();
                if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                    log.warn("Не могу добавить информацию о движении денежных средств {}", row);
                }
            } catch (Exception e) {
                if (!NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                    log.warn("Не могу добавить информацию о движении денежных средств {}", row, e);
                }
            }
        }
    }

    private void addTransaction(TransactionTable transactionTable) {
        for (TransactionTable.Row row : transactionTable.getData()) {
            try {
                Transaction transaction = Transaction.builder()
                        .id(row.getTransactionId())
                        .isin(row.getIsin())
                        .timestamp(row.getTimestamp())
                        .count(row.getCount())
                        .build();
                HttpStatus status = transactionRestController.post(transaction).getStatusCode();
                if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                    log.warn("Не могу добавить транзакцию {}", row);
                }

                TransactionCashFlow.TransactionCashFlowBuilder builder = TransactionCashFlow.builder()
                        .transactionId(row.getTransactionId())
                        .currency(row.getCurrency());

                if (!row.getValue().equals(BigDecimal.ZERO)) {
                    TransactionCashFlow transactionCashFlow = builder
                            .eventType(CashFlowEvent.PRICE)
                            .value(row.getValue())
                            .build();
                    status = transactionCashFlowRestController.post(transactionCashFlow).getStatusCode();
                    if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                        log.warn("Не могу добавить информацию о передвижении средств {}", transactionCashFlow);
                    }
                }

                if (!row.getAccruedInterest().equals(BigDecimal.ZERO)) {
                    TransactionCashFlow transactionCashFlow = builder
                            .eventType(CashFlowEvent.ACCRUED_INTEREST)
                            .value(row.getAccruedInterest())
                            .build();
                    status = transactionCashFlowRestController.post(transactionCashFlow).getStatusCode();
                    if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                        log.warn("Не могу добавить информацию о передвижении средств {}", transactionCashFlow);
                    }
                }

                if (!row.getCommission().equals(BigDecimal.ZERO)) {
                    TransactionCashFlow transactionCashFlow = builder
                            .eventType(CashFlowEvent.COMMISSION)
                            .value(row.getCommission())
                            .build();
                    status = transactionCashFlowRestController.post(transactionCashFlow).getStatusCode();
                    if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                        log.warn("Не могу добавить информацию о передвижении средств {}", transactionCashFlow);
                    }
                }
            } catch (Exception e) {
                log.warn("Не могу добавить транзакцию {}", row, e);
            }
        }
    }

    private void addCouponAndAmortizationCashFlows(CouponAndAmortizationTable couponAndAmortizationTable) {
        for (CouponAndAmortizationTable.Row row : couponAndAmortizationTable.getData()) {
            try {
                EventCashFlow eventCashFlow = EventCashFlow.builder()
                        .isin(row.getIsin())
                        .count(row.getCount())
                        .eventType(row.getEvent())
                        .timestamp(row.getTimestamp())
                        .value(row.getValue())
                        .currency(row.getCurrency())
                        .build();
                HttpStatus status = eventCashFlowRestController.post(eventCashFlow).getStatusCode();
                if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                    log.warn("Не могу добавить информацию о движении денежных средств {}", row);
                }
            } catch (Exception e) {
                log.warn("Не могу добавить информацию о движении денежных средств {}", row, e);
            }
        }
    }

    private void addDividendCashFlows(DividendTable dividendTable) {
        for (DividendTable.Row row : dividendTable.getData()) {
            try {
                EventCashFlow eventCashFlow = EventCashFlow.builder()
                        .isin(row.getIsin())
                        .count(row.getCount())
                        .eventType(row.getEvent())
                        .timestamp(row.getTimestamp())
                        .value(row.getValue())
                        .currency(row.getCurrency())
                        .build();
                HttpStatus status = eventCashFlowRestController.post(eventCashFlow).getStatusCode();
                if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                    log.warn("Не могу добавить информацию о движении денежных средств {}", row);
                }
            } catch (Exception e) {
                log.warn("Не могу добавить информацию о движении денежных средств {}", row, e);
            }
        }
    }

    private void addDerivativeTransaction(DerivativeTransactionTable derivativeTransactionTable) {
        for (DerivativeTransactionTable.Row row : derivativeTransactionTable.getData()) {
            try {
                Security security = Security.builder()
                        .isin(row.getIsin())
                        .name(row.getIsin())
                        .build();
                HttpStatus status = securityRestController.post(security).getStatusCode();
                if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                    log.warn("Не могу добавить ЦБ {} в список", row);
                }
            } catch (Exception e) {
                log.warn("Не могу добавить ЦБ {} в список", row, e);
            }

            try {
                Transaction transaction = Transaction.builder()
                        .id(row.getTransactionId())
                        .isin(row.getIsin())
                        .timestamp(row.getTimestamp())
                        .count(row.getCount())
                        .build();
                HttpStatus status = transactionRestController.post(transaction).getStatusCode();
                if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                    log.warn("Не могу добавить транзакцию {}", row);
                }

                TransactionCashFlow.TransactionCashFlowBuilder builder = TransactionCashFlow.builder()
                        .transactionId(row.getTransactionId())
                        .currency(row.getCurrency());

                if (!row.getValue().equals(BigDecimal.ZERO)) {
                    TransactionCashFlow transactionCashFlow = builder
                            .eventType(CashFlowEvent.DERIVATIVE_PRICE)
                            .value(row.getValue())
                            .build();
                    status = transactionCashFlowRestController.post(transactionCashFlow).getStatusCode();
                    if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                        log.warn("Не могу добавить информацию о передвижении средств {}", transactionCashFlow);
                    }
                }

                if (!row.getCommission().equals(BigDecimal.ZERO)) {
                    TransactionCashFlow transactionCashFlow = builder
                            .eventType(CashFlowEvent.COMMISSION)
                            .value(row.getCommission())
                            .build();
                    status = transactionCashFlowRestController.post(transactionCashFlow).getStatusCode();
                    if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                        log.warn("Не могу добавить информацию о передвижении средств {}", transactionCashFlow);
                    }
                }
            } catch (Exception e) {
                log.warn("Не могу добавить транзакцию {}", row, e);
            }
        }
    }
}

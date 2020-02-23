package ru.portfolio.portfolio.parser.psb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.portfolio.portfolio.controller.EventCashFlowController;
import ru.portfolio.portfolio.controller.SecurityRestController;
import ru.portfolio.portfolio.controller.TransactionCashFlowController;
import ru.portfolio.portfolio.controller.TransactionController;
import ru.portfolio.portfolio.pojo.*;

import java.io.IOException;
import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class PsbReportParserService {
    private final SecurityRestController securityRestController;
    private final EventCashFlowController eventCashFlowController;
    private final TransactionController transactionController;
    private final TransactionCashFlowController transactionCashFlowController;

    public void parse(String reportFile) {
        try (PsbBrokerReport report = new PsbBrokerReport(reportFile)) {
            CashTable cashTable = new CashTable(report);
            CashFlowTable cashFlowTable = new CashFlowTable(report);
            PortfolioTable portfolioTable = new PortfolioTable(report);
            TransactionTable transactionTable = new TransactionTable(report);
            CouponAndAmortizationTable couponAndAmortizationTable = new CouponAndAmortizationTable(report);
            DividendTable dividendTable = new DividendTable(report);

            addSecurities(portfolioTable);
            addCashFlows(cashFlowTable);
            addTransaction(transactionTable);
        } catch (IOException e) {
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
                    log.warn("Не могу добавить ЦБ '{}' в список", row.getIsin());
                }
            } catch (Exception e) {
                log.warn("Не могу добавить ЦБ '{}' в список", row.getIsin(), e);
            }
        }
    }

    private void addCashFlows(CashFlowTable cashFlowTable) {
        for (CashFlowTable.Row row : cashFlowTable.getData()) {
            try {
                EventCashFlow eventCashFlow = EventCashFlow.builder()
                        .eventType(CashFlowEvent.CASH)
                        .timestamp(row.getTimestamp())
                        .value(row.getValue())
                        .currency(row.getCurrency())
                        .build();
                HttpStatus status = eventCashFlowController.post(eventCashFlow).getStatusCode();
                if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                    log.warn("Не могу добавить движение денежных средств от '{}'", row.getTimestamp());
                }
            } catch (Exception e) {
                if (!NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                    log.warn("Не могу добавить движение денежных средств от '{}'", row.getTimestamp(), e);
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
                HttpStatus status = transactionController.post(transaction).getStatusCode();
                if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                    log.warn("Не могу добавить транзакцию № {}", row.getTransactionId());
                }

                TransactionCashFlow.TransactionCashFlowBuilder builder = TransactionCashFlow.builder()
                        .transactionId(row.getTransactionId())
                        .currency(row.getCurrency());

                if (!row.getValue().equals(BigDecimal.ZERO)) {
                    TransactionCashFlow transactionCashFlow = builder
                            .eventType(CashFlowEvent.PRICE)
                            .value(row.getValue())
                            .build();
                    status = transactionCashFlowController.post(transactionCashFlow).getStatusCode();
                    if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                        log.warn("Не могу добавить информацию о передвижении средств {} {} транзакции № {}",
                                transactionCashFlow.getValue(), transactionCashFlow.getCurrency(), row.getTransactionId());
                    }
                }

                if (!row.getAccruedInterest().equals(BigDecimal.ZERO)) {
                    TransactionCashFlow transactionCashFlow = builder
                            .eventType(CashFlowEvent.ACCRUED_INTEREST)
                            .value(row.getAccruedInterest())
                            .build();
                    status = transactionCashFlowController.post(transactionCashFlow).getStatusCode();
                    if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                        log.warn("Не могу добавить информацию о передвижении средств {} {} транзакции № {}",
                                transactionCashFlow.getValue(), transactionCashFlow.getCurrency(), row.getTransactionId());
                    }
                }

                if (!row.getCommission().equals(BigDecimal.ZERO)) {
                    TransactionCashFlow transactionCashFlow = builder
                            .eventType(CashFlowEvent.COMMISSION)
                            .value(row.getCommission())
                            .build();
                    status = transactionCashFlowController.post(transactionCashFlow).getStatusCode();
                    if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                        log.warn("Не могу добавить информацию о передвижении средств {} {} транзакции № {}",
                                transactionCashFlow.getValue(), transactionCashFlow.getCurrency(), row.getTransactionId());
                    }
                }
            } catch (Exception e) {
                log.warn("Не могу добавить транзакцию № {}", row.getTransactionId(), e);
            }
        }
    }
}

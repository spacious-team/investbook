package ru.portfolio.portfolio.parser.psb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.portfolio.portfolio.controller.EventCashFlowController;
import ru.portfolio.portfolio.controller.SecurityRestController;
import ru.portfolio.portfolio.pojo.CashFlowEvent;
import ru.portfolio.portfolio.pojo.EventCashFlow;
import ru.portfolio.portfolio.pojo.Security;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class PsbReportParserService {
    private final SecurityRestController securityRestController;
    private final EventCashFlowController eventCashFlowController;

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
                if (!status.is2xxSuccessful() || status != HttpStatus.CONFLICT) {
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
                if (!status.is2xxSuccessful() || status != HttpStatus.CONFLICT) {
                    log.warn("Не могу добавить движение денежных средств от '{}'", row.getTimestamp());
                }
            } catch (Exception e) {
                log.warn("Не могу добавить движение денежных средств от '{}'", row.getTimestamp(), e);
            }
        }
    }
}

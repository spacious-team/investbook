package ru.portfolio.portfolio.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.SecurityEventCashFlowConverter;
import ru.portfolio.portfolio.converter.TransactionCashFlowConverter;
import ru.portfolio.portfolio.converter.TransactionConverter;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;
import ru.portfolio.portfolio.pojo.Transaction;
import ru.portfolio.portfolio.pojo.TransactionCashFlow;
import ru.portfolio.portfolio.repository.SecurityEventCashFlowRepository;
import ru.portfolio.portfolio.repository.TransactionCashFlowRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DerivativeCashFlowFactory {
    private static final ZoneId MOEX_TIMEZONE = ZoneId.of("Europe/Moscow");
    private static final int LAST_TRADE_HOUR = 18;
    private final TransactionRepository transactionRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final TransactionConverter transactionConverter;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;
    private final TransactionCashFlowConverter transactionCashFlowConverter;

    public DerivativeCashFlow getDerivativeCashFlow(PortfolioEntity portfolio, SecurityEntity security) {
        Deque<Transaction> transactions = getTransactions(portfolio, security);
        Deque<SecurityEventCashFlow> securityEventCashFlows = getSecurityEventCashFlows(portfolio, security);

        DerivativeCashFlow derivativeCashFlow = new DerivativeCashFlow();
        BigDecimal totalProfit = BigDecimal.ZERO;
        int currentPosition = 0;
        for (SecurityEventCashFlow cash : securityEventCashFlows) {
            LocalDate currentDay = ZonedDateTime.ofInstant(cash.getTimestamp(), MOEX_TIMEZONE).toLocalDate();
            totalProfit = totalProfit.add(cash.getValue());
            Deque<Transaction> dailyTransactions = getDailyTransactions(transactions, currentDay);
            currentPosition += dailyTransactions.stream()
                    .mapToInt(Transaction::getCount)
                    .sum();

            derivativeCashFlow.getCashFlows().add(
                    DerivativeCashFlow.DailyCashFlow.builder()
                            .dailyTransactions(getCashFlows(dailyTransactions))
                            .dailyProfit(cash)
                            .totalProfit(totalProfit)
                            .position(currentPosition)
                            .build());
        }
        return derivativeCashFlow;
    }

    private LinkedList<Transaction> getTransactions(PortfolioEntity portfolio, SecurityEntity security) {
        return transactionRepository
                .findBySecurityAndPortfolioOrderByTimestampAscIdAsc(security, portfolio)
                .stream()
                .map(transactionConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private Deque<SecurityEventCashFlow> getSecurityEventCashFlows(PortfolioEntity portfolio, SecurityEntity security) {
        return securityEventCashFlowRepository
                    .findByPortfolioAndIsinAndCashFlowTypeOrderByTimestampAsc(
                            portfolio.getPortfolio(),
                            security.getIsin(),
                            CashFlowType.DERIVATIVE_PROFIT)
                    .stream()
                    .map(securityEventCashFlowConverter::fromEntity)
                    .collect(Collectors.toCollection(LinkedList::new));
    }

    private Deque<Transaction> getDailyTransactions(Deque<Transaction> transactions, LocalDate currentDay) {
        return transactions.stream()
                .filter(e -> {
                    LocalDateTime dateTime = ZonedDateTime.ofInstant(e.getTimestamp(), MOEX_TIMEZONE).toLocalDateTime();
                    return currentDay.equals((dateTime.get(ChronoField.HOUR_OF_DAY) <= LAST_TRADE_HOUR) ?
                            dateTime.toLocalDate() :
                            dateTime.toLocalDate().plusDays(1));
                })
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private LinkedHashMap<Transaction, Map<CashFlowType, TransactionCashFlow>> getCashFlows(Deque<Transaction> dailyTransactions) {
        LinkedHashMap<Transaction, Map<CashFlowType, TransactionCashFlow>> dailyTransactionsCashFlows = new LinkedHashMap<>();
        for (Transaction transaction : dailyTransactions) {
            if (transaction.getId() == null) continue;
            dailyTransactionsCashFlows.put(transaction,
                    transactionCashFlowRepository.findByTransactionId(transaction.getId())
                            .stream()
                            .map(transactionCashFlowConverter::fromEntity)
                            .collect(Collectors.toMap(TransactionCashFlow::getEventType, Function.identity())));
        }
        return dailyTransactionsCashFlows;
    }
}

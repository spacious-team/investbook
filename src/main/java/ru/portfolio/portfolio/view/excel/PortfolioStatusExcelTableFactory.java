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

package ru.portfolio.portfolio.view.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.SecurityConverter;
import ru.portfolio.portfolio.converter.TransactionConverter;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.entity.SecurityEventCashFlowEntity;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntity;
import ru.portfolio.portfolio.entity.TransactionEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.Portfolio;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.pojo.Transaction;
import ru.portfolio.portfolio.repository.SecurityEventCashFlowRepository;
import ru.portfolio.portfolio.repository.SecurityRepository;
import ru.portfolio.portfolio.repository.TransactionCashFlowRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;
import ru.portfolio.portfolio.view.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static ru.portfolio.portfolio.view.excel.PortfolioStatusExcelTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioStatusExcelTableFactory implements TableFactory {
    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final SecurityConverter securityConverter;
    private final TransactionConverter transactionConverter;
    private final PaidInterestFactory paidInterestFactory;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final PositionsFactory positionsAndPaidInterestRegistry;

    public Table create(Portfolio portfolio) {
        throw new UnsupportedOperationException();
    }

    public Table create(Portfolio portfolio, String forCurrency) {
        return create(portfolio, getSecuritiesIsin(portfolio, forCurrency));
    }

    public Table create(Portfolio portfolio, Collection<String> securitiesIsin) {
        Table table = new Table();
        for (String isin : securitiesIsin) {
            Optional<SecurityEntity> securityEntity = securityRepository.findByIsin(isin);
            if (securityEntity.isPresent()) {
                Security security = securityConverter.fromEntity(securityEntity.get());
                Table.Record row = getSecurityStatus(portfolio, security);
                table.add(row);
            }
        }
        return table;
    }

    private Table.Record getSecurityStatus(Portfolio portfolio, Security security) {
        Positions positions = positionsAndPaidInterestRegistry.get(portfolio, security);
        PaidInterest paidInterest = paidInterestFactory.get(portfolio, security);
        Table.Record row = new Table.Record();
        row.put(SECURITY,
                Optional.ofNullable(security.getName())
                        .orElse(security.getIsin()));
        row.put(FIRST_TRNSACTION_DATE,
                transactionRepository
                        .findFirstBySecurityIsinAndPkPortfolioOrderByTimestampAsc(security.getIsin(), portfolio.getId())
                        .map(TransactionEntity::getTimestamp)
                        .orElse(null));
        row.put(LAST_TRANSACTION_DATE,
                transactionRepository
                        .findFirstBySecurityIsinAndPkPortfolioOrderByTimestampDesc(security.getIsin(), portfolio.getId())
                        .map(TransactionEntity::getTimestamp)
                        .orElse(null));
        row.put(LAST_EVENT_DATE,
                securityEventCashFlowRepository
                        .findFirstByPortfolioIdAndSecurityIsinAndCashFlowTypeIdInOrderByTimestampDesc(
                                portfolio.getId(), security.getIsin(), Set.of(
                                        CashFlowType.AMORTIZATION.getId(),
                                        CashFlowType.REDEMPTION.getId(),
                                        CashFlowType.COUPON.getId(),
                                        CashFlowType.DIVIDEND.getId()))
                        .map(SecurityEventCashFlowEntity::getTimestamp)
                        .orElse(null));
        row.put(BUY_COUNT, Optional.ofNullable(
                transactionRepository.findBySecurityIsinAndPkPortfolioBuyCount(security, portfolio))
                .orElse(0L));
        row.put(CELL_COUNT, Optional.ofNullable(
                transactionRepository.findBySecurityIsinAndPkPortfolioCellCount(security, portfolio))
                .orElse(0L));
        Integer count = Optional.ofNullable(positions.getPositionHistories().peekLast())
                .map(PositionHistory::getOpenedPositions)
                .orElse(0);
        Deque<Transaction> transactions = getTransactions(portfolio, security);
        row.put(COUNT, count);
        if (count != 0) {
            row.put(AVERAGE_PRICE,
                    getTotalAmount(transactions)
                            .divide(BigDecimal.valueOf(Math.abs(count)), 2, RoundingMode.CEILING));
        }
        row.put(COMMISSION, getTotalComission(transactions));
        row.put(COUPON, paidInterest.sumPaymentsForType(CashFlowType.COUPON));
        row.put(AMORTIZATION, paidInterest.sumPaymentsForType(CashFlowType.AMORTIZATION));
        row.put(DIVIDEND, paidInterest.sumPaymentsForType(CashFlowType.DIVIDEND));
        row.put(TAX, paidInterest.sumPaymentsForType(CashFlowType.TAX).abs());
        row.put(PROFIT, "=" + COUPON.getCellAddr() + "+" + AMORTIZATION.getCellAddr() + "+" + DIVIDEND.getCellAddr() +
                "-" + COMMISSION.getCellAddr());
        return row;
    }

    private LinkedList<Transaction> getTransactions(Portfolio portfolio, Security security) {
        return transactionRepository
                .findBySecurityIsinAndPkPortfolioOrderByTimestampAscPkIdAsc(security.getIsin(), portfolio.getId())
                .stream()
                .map(transactionConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private Collection<String> getSecuritiesIsin(Portfolio portfolio, String currency) {
        return transactionRepository.findDistinctIsinByPortfolioAndCurrencyOrderByTimestampDesc(portfolio, currency);
    }

    private BigDecimal getTotalAmount(Deque<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getId() != null && t.getCount() != 0)
                .map(this::getAmount)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();
    }

    private Optional<BigDecimal> getAmount(Transaction t) {
        return transactionCashFlowRepository
                .findByPkPortfolioAndPkTransactionIdAndPkType(t.getPortfolio(), t.getId(), CashFlowType.PRICE.getId())
                .map(TransactionCashFlowEntity::getValue);
    }

    private BigDecimal getTotalComission(Deque<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getId() != null && t.getCount() != 0)
                .map(this::getCommission)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<BigDecimal> getCommission(Transaction t) {
        return transactionCashFlowRepository
                .findByPkPortfolioAndPkTransactionIdAndPkType(t.getPortfolio(), t.getId(), CashFlowType.COMMISSION.getId())
                .map(TransactionCashFlowEntity::getValue)
                .map(BigDecimal::abs);
    }
}

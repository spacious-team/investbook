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
import ru.portfolio.portfolio.entity.SecurityEventCashFlowEntity;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntity;
import ru.portfolio.portfolio.entity.TransactionEntity;
import ru.portfolio.portfolio.pojo.*;
import ru.portfolio.portfolio.repository.SecurityEventCashFlowRepository;
import ru.portfolio.portfolio.repository.SecurityRepository;
import ru.portfolio.portfolio.repository.TransactionCashFlowRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;
import ru.portfolio.portfolio.view.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static ru.portfolio.portfolio.pojo.SecurityType.getCurrencyPair;
import static ru.portfolio.portfolio.pojo.SecurityType.getSecurityType;
import static ru.portfolio.portfolio.view.excel.PortfolioStatusExcelTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioStatusExcelTableFactory implements TableFactory {
    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final SecurityConverter securityConverter;
    private final PaidInterestFactory paidInterestFactory;
    private final PositionsFactory positionsFactory;

    public Table create(Portfolio portfolio) {
        throw new UnsupportedOperationException();
    }

    public Table create(Portfolio portfolio, String forCurrency) {
        return create(portfolio, getSecuritiesIsin(portfolio, forCurrency));
    }

    public Table create(Portfolio portfolio, Collection<String> securitiesIsin) {
        Table table = new Table();
        for (String isin : securitiesIsin) {
            Optional<Security> security = getSecurity(isin);
            if (security.isPresent()) {
                Table.Record row = getSecurityStatus(portfolio, security.get());
                table.add(row);
            }
        }
        return table;
    }

    private Collection<String> getSecuritiesIsin(Portfolio portfolio, String currency) {
        Collection<String> contracts = new ArrayList<>();
        contracts.addAll(transactionRepository.findDistinctIsinByPortfolioAndCurrencyOrderByTimestampDesc(portfolio, currency));
        contracts.addAll(transactionRepository.findDistinctFxCurrencyPairs(portfolio, currency));
        if (currency.equalsIgnoreCase("RUB")) {
            contracts.addAll(transactionRepository.findDistinctDerivativeByPortfolioOrderByTimestampDesc(portfolio));
        }
        return contracts;
    }

    private Optional<Security> getSecurity(String isin) {
        if (getSecurityType(isin) == SecurityType.CURRENCY_PAIR && isin.length() == 6) {
            Optional<Security> security = getSecurity(isin + "_TOM");
            if (security.isEmpty()) {
                security = getSecurity(isin + "_TOD");
            }
            return security;
        }
        return securityRepository.findByIsin(isin)
                .map(securityConverter::fromEntity);
    }

    private Table.Record getSecurityStatus(Portfolio portfolio, Security security) {
        Positions positions = positionsFactory.get(portfolio, security);
        PaidInterest paidInterest = paidInterestFactory.get(portfolio, security);
        Table.Record row = new Table.Record();
        row.put(SECURITY,
                Optional.ofNullable(security.getName())
                        .orElse((getSecurityType(security) == SecurityType.CURRENCY_PAIR) ?
                                getCurrencyPair(security.getIsin()) :
                                security.getIsin()));
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
                                        CashFlowType.DIVIDEND.getId(),
                                        CashFlowType.DERIVATIVE_PROFIT.getId()))
                        .map(SecurityEventCashFlowEntity::getTimestamp)
                        .orElse(null));
        row.put(BUY_COUNT, Optional.ofNullable(
                transactionRepository.findBySecurityIsinAndPkPortfolioBuyCount(security, portfolio))
                .orElse(0L));
        row.put(CELL_COUNT, Optional.ofNullable(
                transactionRepository.findBySecurityIsinAndPkPortfolioCellCount(security, portfolio))
                .orElse(0L) +
                positions.getRedemptions().size());
        int count = getCount(positions);
        row.put(COUNT, count);
        if (count == 0) {
            row.put(GROSS_PROFIT, getGrossProfit(portfolio, security, positions));
        } else {
            row.put(AVERAGE_PRICE, getPurchaseCost(security, positions)
                    .abs()
                    .divide(BigDecimal.valueOf(Math.max(1, Math.abs(count))), 2, RoundingMode.CEILING));
            row.put(AVERAGE_ACCRUED_INTEREST, getPurchaseAccruedInterest(security, positions)
                    .abs()
                    .divide(BigDecimal.valueOf(Math.max(1, Math.abs(count))), 2, RoundingMode.CEILING));
        }
        row.put(COMMISSION, getTotal(positions.getTransactions(), CashFlowType.COMMISSION).abs());
        row.put(COUPON, paidInterest.sumPaymentsForType(CashFlowType.COUPON));
        row.put(AMORTIZATION, paidInterest.sumPaymentsForType(CashFlowType.AMORTIZATION));
        row.put(DIVIDEND, paidInterest.sumPaymentsForType(CashFlowType.DIVIDEND));
        row.put(TAX, paidInterest.sumPaymentsForType(CashFlowType.TAX).abs());
        row.put(PROFIT, "=" + COUPON.getCellAddr() + "+" + AMORTIZATION.getCellAddr() + "+" + DIVIDEND.getCellAddr() +
                "+" + GROSS_PROFIT.getCellAddr() + "-" + TAX.getCellAddr() + "-" + COMMISSION.getCellAddr());
        return row;
    }

    private int getCount(Positions positions) {
        return Optional.ofNullable(positions.getPositionHistories().peekLast())
                .map(PositionHistory::getOpenedPositions)
                .orElse(0);
    }

    /**
     * Курсовой доход с купли-продажи (для деривативов - суммарная вариационная маржа)
     */
    private BigDecimal getGrossProfit(Portfolio portfolio, Security security, Positions positions) {
        SecurityType securityType = getSecurityType(security);
        switch (securityType) {
            case STOCK_OR_BOND:
                return  getPurchaseCost(security, positions)
                        .add(getPurchaseAccruedInterest(security, positions));
            case DERIVATIVE:
                return getDerivativeProfit(portfolio, security);
            case CURRENCY_PAIR:
                return  getPurchaseCost(security, positions);
        }
        throw new IllegalArgumentException("Не поддерживаемый тип ценной бумаги " + security);
    }

    /**
     * Разница доходов с продажи и расходов на покупку
     */
    private BigDecimal getPurchaseCost(Security security, Positions positions) {
        SecurityType securityType = getSecurityType(security);
        switch (securityType) {
            case STOCK_OR_BOND:
                BigDecimal purchaseCost = getTotal(positions.getTransactions(), CashFlowType.PRICE);
                return positions.getRedemptions()
                        .stream()
                        .map(SecurityEventCashFlow::getValue)
                        .map(BigDecimal::abs)
                        .reduce(purchaseCost, BigDecimal::add);
            case DERIVATIVE:
                return getTotal(positions.getTransactions(), CashFlowType.DERIVATIVE_PRICE);
            case CURRENCY_PAIR:
                return getTotal(positions.getTransactions(), CashFlowType.PRICE);
        }
        throw new IllegalArgumentException("Не поддерживаемый тип ценной бумаги " + security);
    }

    /**
     * Разница проданного и купленного НКД
     */
    private BigDecimal getPurchaseAccruedInterest(Security security, Positions positions) {
        if (getSecurityType(security) == SecurityType.STOCK_OR_BOND) {
            return getTotal(positions.getTransactions(), CashFlowType.ACCRUED_INTEREST);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getTotal(Deque<Transaction> transactions, CashFlowType type) {
        return transactions.stream()
                .filter(t -> t.getId() != null && t.getCount() != 0)
                .map(t -> getTransactionValue(t, type))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<BigDecimal> getTransactionValue(Transaction t, CashFlowType type) {
        return transactionCashFlowRepository
                .findByPkPortfolioAndPkTransactionIdAndPkType(t.getPortfolio(), t.getId(), type.getId())
                .map(TransactionCashFlowEntity::getValue);
    }

    private BigDecimal getDerivativeProfit(Portfolio portfolio, Security contract) {
        return securityEventCashFlowRepository
                .findByPortfolioIdAndSecurityIsinAndCashFlowTypeIdOrderByTimestampAsc(
                        portfolio.getId(), contract.getIsin(), CashFlowType.DERIVATIVE_PROFIT.getId())
                .stream()
                .map(SecurityEventCashFlowEntity::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

/*
 * InvestBook
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

package ru.investbook.view.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.pojo.*;
import ru.investbook.repository.*;
import ru.investbook.view.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static ru.investbook.pojo.SecurityType.getCurrencyPair;
import static ru.investbook.pojo.SecurityType.getSecurityType;
import static ru.investbook.view.excel.PortfolioStatusExcelTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioStatusExcelTableFactory implements TableFactory {
    private static final String STOCK_GROSS_PROFIT_FORMULA = getStockOrBondGrossProfitFormula();
    private static final String PROFIT_PROPORTION_FORMULA = getProfitProportionFormula();
    private static final String PROPORTION_FORMULA = getProportionFormula();
    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final SecurityQuoteRepository securityQuoteRepository;
    private final SecurityConverter securityConverter;
    private final PositionsFactory positionsFactory;
    private final ForeignExchangeRateService foreignExchangeRateService;

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
        contracts.addAll(
                transactionRepository.findDistinctIsinByPortfolioAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
                        portfolio,
                        currency,
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate()));
        contracts.addAll(
                transactionRepository.findDistinctFxCurrencyPairsAndTimestampBetween(
                        portfolio,
                        currency,
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate()));
        if (currency.equalsIgnoreCase("RUB")) {
            contracts.addAll(
                    transactionRepository.findDistinctDerivativeByPortfolioAndTimestampBetweenOrderByTimestampDesc(
                            portfolio,
                            ViewFilter.get().getFromDate(),
                            ViewFilter.get().getToDate()));
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
        Table.Record row = new Table.Record();
        SecurityType securityType = getSecurityType(security);
        row.put(SECURITY,
                Optional.ofNullable(security.getName())
                        .orElse((securityType == SecurityType.CURRENCY_PAIR) ?
                                getCurrencyPair(security.getIsin()) :
                                security.getIsin()));
        try {
            Positions positions = positionsFactory.get(portfolio, security, ViewFilter.get());
            row.put(FIRST_TRANSACTION_DATE,
                    transactionRepository
                            .findFirstBySecurityIsinAndPkPortfolioAndTimestampBetweenOrderByTimestampAsc(
                                    security.getIsin(),
                                    portfolio.getId(),
                                    ViewFilter.get().getFromDate(),
                                    ViewFilter.get().getToDate())
                            .map(TransactionEntity::getTimestamp)
                            .orElse(null));
            row.put(LAST_TRANSACTION_DATE,
                    transactionRepository
                            .findFirstBySecurityIsinAndPkPortfolioAndTimestampBetweenOrderByTimestampDesc(
                                    security.getIsin(),
                                    portfolio.getId(),
                                    ViewFilter.get().getFromDate(),
                                    ViewFilter.get().getToDate())
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
                    transactionRepository.findBySecurityIsinAndPkPortfolioAndTimestampBetweenBuyCount(
                            security,
                            portfolio,
                            ViewFilter.get().getFromDate(),
                            ViewFilter.get().getToDate()))
                    .orElse(0L));
            row.put(CELL_COUNT, Optional.ofNullable(
                    transactionRepository.findBySecurityIsinAndPkPortfolioAndTimestampBetweenCellCount(
                            security,
                            portfolio,
                            ViewFilter.get().getFromDate(),
                            ViewFilter.get().getToDate()))
                    .orElse(0L) +
                    positions.getRedemptions()
                            .stream()
                            .mapToInt(SecurityEventCashFlow::getCount)
                            .sum());
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

                if (securityType == SecurityType.STOCK_OR_BOND) {
                    securityQuoteRepository.findFirstBySecurityIsinOrderByTimestampDesc(security.getIsin())
                            .ifPresent(quote -> {
                                row.put(CURRENT_PRICE, Optional.ofNullable(quote.getPrice()) // for bonds
                                        .orElse(quote.getQuote())); // for stocks
                                row.put(CURRENT_ACCRUED_INTEREST, quote.getAccruedInterest());
                            });
                } else if (securityType == SecurityType.CURRENCY_PAIR) {
                    row.put(CURRENT_PRICE,
                            foreignExchangeRateService.getExchangeRateToRub(getCurrencyPair(security.getIsin()).substring(0, 3)));
                }

                if (securityType == SecurityType.STOCK_OR_BOND || securityType == SecurityType.CURRENCY_PAIR) {
                    row.put(GROSS_PROFIT, STOCK_GROSS_PROFIT_FORMULA);
                } else if (securityType == SecurityType.DERIVATIVE) {
                    row.put(GROSS_PROFIT, getGrossProfit(portfolio, security, positions));
                }
                if (securityType == SecurityType.STOCK_OR_BOND || securityType == SecurityType.CURRENCY_PAIR) {
                    row.put(PROPORTION, PROPORTION_FORMULA);
                }
            }
            row.put(COMMISSION, getTotal(positions.getTransactions(), CashFlowType.COMMISSION).abs());
            row.put(COUPON, sumPaymentsForType(portfolio, security, CashFlowType.COUPON));
            row.put(AMORTIZATION, sumPaymentsForType(portfolio, security, CashFlowType.AMORTIZATION));
            row.put(DIVIDEND, sumPaymentsForType(portfolio, security, CashFlowType.DIVIDEND));
            row.put(TAX, sumPaymentsForType(portfolio, security, CashFlowType.TAX).abs());
            row.put(PROFIT, "=" + COUPON.getCellAddr() + "+" + AMORTIZATION.getCellAddr() + "+" + DIVIDEND.getCellAddr() +
                    "+" + GROSS_PROFIT.getCellAddr() + "-" + TAX.getCellAddr() + "-" + COMMISSION.getCellAddr());
            row.put(PROFIT_PROPORTION, PROFIT_PROPORTION_FORMULA);
        } catch (Exception e) {
            log.error("Ошибка при формировании агрегированных данных по бумаге {}", security, e);
        }
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
        return switch (securityType) {
            case STOCK_OR_BOND -> getPurchaseCost(security, positions)
                    .add(getPurchaseAccruedInterest(security, positions));
            case DERIVATIVE -> getDerivativeProfit(portfolio, security);
            case CURRENCY_PAIR -> getPurchaseCost(security, positions);
        };
    }

    /**
     * Разница доходов с продажи и расходов на покупку
     */
    private BigDecimal getPurchaseCost(Security security, Positions positions) {
        SecurityType securityType = getSecurityType(security);
        return switch (securityType) {
            case STOCK_OR_BOND -> getStockOrBondPurchaseCost(positions);
            case DERIVATIVE -> getTotal(positions.getTransactions(), CashFlowType.DERIVATIVE_PRICE);
            case CURRENCY_PAIR -> getTotal(positions.getTransactions(), CashFlowType.PRICE);
        };
    }

    /**
     * Разница цен продаж и покупок. Не учитывается цена покупки, если ЦБ выведена со счета, не учитывается цена
     * продажи, если ЦБ введена на счет
     */
    private BigDecimal getStockOrBondPurchaseCost(Positions positions) {
        BigDecimal purchaseCost = positions.getOpenedPositions()
                .stream()
                .map(openPosition -> getTransactionValue(openPosition.getOpenTransaction(), CashFlowType.PRICE)
                        .map(value -> value.multiply(getOpenAmountMultiplier(openPosition))))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add); // если ценная бумага не вводилась на счет, а была куплена (есть цена покупки)
        for (ClosedPosition closedPosition : positions.getClosedPositions()) {
            BigDecimal openPrice = getTransactionValue(closedPosition.getOpenTransaction(), CashFlowType.PRICE)
                    .map(value -> value.multiply(getOpenAmountMultiplier(closedPosition)))
                    .orElse(null);
            BigDecimal closePrice = getTransactionValue(closedPosition.getCloseTransaction(), CashFlowType.PRICE)
                    .map(value -> value.multiply(getClosedAmountMultiplier(closedPosition)))
                    // redemption closing price will be taken into account later
                    .orElseGet(() -> (closedPosition.getClosingEvent() == CashFlowType.REDEMPTION) ? BigDecimal.ZERO : null);
            if (openPrice != null && closePrice != null) {
                // если ценная бумага не вводилась и не выводилась со счета, а была куплена и продана
                // (есть цены покупки и продажи)
                purchaseCost = purchaseCost.add(openPrice).add(closePrice);
            }
        }
        return positions.getRedemptions()
                .stream()
                .map(SecurityEventCashFlow::getValue)
                .map(BigDecimal::abs)
                .reduce(purchaseCost, BigDecimal::add);
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
        if (t.getId() == null) { // redemption
            return Optional.empty();
        }
        return transactionCashFlowRepository
                .findByPkPortfolioAndPkTransactionIdAndPkType(t.getPortfolio(), t.getId(), type.getId())
                .map(TransactionCashFlowEntity::getValue);
    }

    private BigDecimal getDerivativeProfit(Portfolio portfolio, Security contract) {
        return securityEventCashFlowRepository
                .findByPortfolioIdAndSecurityIsinAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                        portfolio.getId(),
                        contract.getIsin(),
                        CashFlowType.DERIVATIVE_PROFIT.getId(),
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate())
                .stream()
                .map(SecurityEventCashFlowEntity::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getOpenAmountMultiplier(OpenedPosition openedPosition) {
        int positionCount = Math.abs(openedPosition.getCount());
        int transactionCount = Math.abs(openedPosition.getOpenTransaction().getCount());
        if (positionCount == transactionCount) {
            return BigDecimal.ONE;
        } else {
            return BigDecimal.valueOf(positionCount)
                    .divide(BigDecimal.valueOf(transactionCount), 6, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal getClosedAmountMultiplier(ClosedPosition closedPosition) {
        int positionCount = Math.abs(closedPosition.getCount());
        int transactionCount = Math.abs(closedPosition.getCloseTransaction().getCount());
        if (positionCount == transactionCount) {
            return BigDecimal.ONE;
        } else {
            return BigDecimal.valueOf(positionCount)
                    .divide(BigDecimal.valueOf(transactionCount), 6, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal sumPaymentsForType(Portfolio portfolio, Security security, CashFlowType cashFlowType) {
        return securityEventCashFlowRepository
                .findByPortfolioIdAndSecurityIsinAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                        portfolio.getId(),
                        security.getIsin(),
                        cashFlowType.getId(),
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate())
                .stream()
                .map(SecurityEventCashFlowEntity::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String getStockOrBondGrossProfitFormula() {
        return "=IF(" + CURRENT_PRICE.getCellAddr() + "<>\"\",(" +
                CURRENT_PRICE.getCellAddr() + "+" + CURRENT_ACCRUED_INTEREST.getCellAddr() + "-" +
                AVERAGE_PRICE.getCellAddr() + "-" + AVERAGE_ACCRUED_INTEREST.getCellAddr() + ")*" +
                COUNT.getCellAddr() + ",0)";
    }

    private static String getProfitProportionFormula() {
        return "=" + PROFIT.getCellAddr() + "/" + PROFIT.getColumnIndex() + "2";
    }

    private static String getProportionFormula() {
        return "=IF(" + COUNT.getCellAddr() + ">0,1,0)*" +
                "((" + AVERAGE_PRICE.getCellAddr() + "+" + AVERAGE_ACCRUED_INTEREST.getCellAddr() + ")*" + COUNT.getCellAddr() +
                "-" + AMORTIZATION.getCellAddr() + ")" +
                "/(SUMPRODUCT((0+" + AVERAGE_PRICE.getColumnIndex() + "3:" + AVERAGE_PRICE.getColumnIndex() + "100000)," +
                "(0+" + COUNT.getColumnIndex() + "3:" + COUNT.getColumnIndex() + "100000)," +
                "SIGN(" + COUNT.getColumnIndex() + "3:" + COUNT.getColumnIndex() + "100000>0))" +
                "+SUMPRODUCT((0+" + AVERAGE_ACCRUED_INTEREST.getColumnIndex() + "3:" + AVERAGE_ACCRUED_INTEREST.getColumnIndex() + "100000)," +
                "(0+" + COUNT.getColumnIndex() + "3:" + COUNT.getColumnIndex() + "100000)," +
                "SIGN(" + COUNT.getColumnIndex() + "3:" + COUNT.getColumnIndex() + "100000>0))" +
                "-SUMIF(" + COUNT.getColumnIndex() + "3:" + COUNT.getColumnIndex() + "100000,\">0\"," +
                AMORTIZATION.getColumnIndex() + "3:" + AMORTIZATION.getColumnIndex() + "100000))";
    }
}

/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.view.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.*;
import org.spacious_team.broker.report_parser.api.PortfolioCash;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioPropertyConverter;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.repository.*;
import ru.investbook.view.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static org.spacious_team.broker.pojo.SecurityType.*;
import static ru.investbook.view.excel.PortfolioStatusExcelTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioStatusExcelTableFactory implements TableFactory {
    static final BigDecimal minCash = BigDecimal.valueOf(0.01);
    private static final String STOCK_GROSS_PROFIT_FORMULA = getStockOrBondGrossProfitFormula();
    private static final String PROFIT_FORMULA = getProfitFormula();
    private static final String PROFIT_PROPORTION_FORMULA = getProfitProportionFormula();
    private static final String INVESTMENT_PROPORTION_FORMULA = getInvestmentProportionFormula();
    private static final String PROPORTION_FORMULA = getProportionFormula();
    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final SecurityQuoteRepository securityQuoteRepository;
    private final SecurityConverter securityConverter;
    private final PortfolioPropertyConverter portfolioPropertyConverter;
    private final PositionsFactory positionsFactory;
    private final ForeignExchangeRateService foreignExchangeRateService;
    private final PortfolioPropertyRepository portfolioPropertyRepository;
    private final Instant instantOf2000_01_01 = LocalDate.of(2000, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();

    public Table create(Portfolio portfolio) {
        throw new UnsupportedOperationException();
    }

    public Table create(Portfolio portfolio, String forCurrency) {
        Table table = create(portfolio, getSecuritiesIsin(portfolio, forCurrency));
        table.add(getCashRow(portfolio, forCurrency));
        return table;
    }

    private Table create(Portfolio portfolio, Collection<String> securitiesIsin) {
        Table table = new Table();
        for (String isin : securitiesIsin) {
            getSecurity(isin)
                    .map(security -> getSecurityStatus(portfolio, security))
                    .ifPresent(table::add);
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
                transactionRepository.findDistinctFxCurrencyPairByPortfolioAndCurrencyAndTimestampBetween(
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

    private Table.Record getCashRow(Portfolio portfolio, String forCurrency) {
        Table.Record row = new Table.Record();
        Instant atTime = Instant.ofEpochSecond(Math.min(
                ViewFilter.get().getToDate().getEpochSecond(),
                Instant.now().getEpochSecond()));
        row.put(SECURITY, "Остаток денежных средств, " + forCurrency.toLowerCase());
        Optional<PortfolioProperty> portfolioCashes = getPortfolioCash(portfolio, atTime);
        row.put(LAST_EVENT_DATE, portfolioCashes.map(PortfolioProperty::getTimestamp).orElse(null));
        BigDecimal portfolioCash = portfolioCashes.map(portfolioProperty ->
                PortfolioCash.valueOf(portfolioProperty.getValue())
                        .stream()
                        .filter(cash -> forCurrency.equals(cash.getCurrency()))
                        .map(PortfolioCash::getValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElse(null);
        row.put(LAST_PRICE, portfolioCash);
        if (ViewFilter.get().getFromDate().isBefore(instantOf2000_01_01) &&
                portfolioCash != null && portfolioCash.compareTo(minCash) >= 1) { // fix div by zero in proportion column
            // режим отображения по умолчанию, скорее всего отображаем портфель с начала открытия счета,
            // учитываем остаток денежных средств в Доле портфеля (%)
            row.put(PROPORTION, PROPORTION_FORMULA);
            row.put(COUNT, 1);
        }
        return row;
    }

    private Optional<Security> getSecurity(String isin) {
        if (getSecurityType(isin) == CURRENCY_PAIR) {
            return Optional.of(Security.builder()
                    .isin(SecurityType.getCurrencyPair(isin))
                    .build());
        } else {
            return securityRepository.findByIsin(isin)
                    .map(securityConverter::fromEntity);
        }
    }

    private Table.Record getSecurityStatus(Portfolio portfolio, Security security) {
        Table.Record row = new Table.Record();
        SecurityType securityType = getSecurityType(security);
        row.put(SECURITY,
                Optional.ofNullable(security.getName())
                        .orElse((securityType == CURRENCY_PAIR) ?
                                getCurrencyPair(security.getIsin()) :
                                security.getIsin()));
        row.put(TYPE, securityType.getDescription());
        try {
            ViewFilter filter = ViewFilter.get();
            Positions positions = positionsFactory.get(portfolio, security, filter);
            row.put(FIRST_TRANSACTION_DATE, Optional.ofNullable(positions.getPositionHistories().peekFirst())
                    .map(PositionHistory::getInstant)
                    .orElse(null));
            row.put(LAST_TRANSACTION_DATE, Optional.ofNullable(positions.getPositionHistories().peekLast())
                    .map(PositionHistory::getInstant)
                    .orElse(null));
            if (securityType != CURRENCY_PAIR) {
                row.put(LAST_EVENT_DATE,
                        securityEventCashFlowRepository
                                .findFirstByPortfolioIdAndSecurityIsinAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
                                        portfolio.getId(), security.getIsin(), Set.of(
                                                CashFlowType.AMORTIZATION.getId(),
                                                CashFlowType.REDEMPTION.getId(),
                                                CashFlowType.COUPON.getId(),
                                                CashFlowType.DIVIDEND.getId(),
                                                CashFlowType.DERIVATIVE_PROFIT.getId()),
                                        filter.getFromDate(), filter.getToDate())
                                .map(SecurityEventCashFlowEntity::getTimestamp)
                                .orElse(null));
            }
            row.put(BUY_COUNT, positions.getTransactions()
                    .stream()
                    .mapToInt(Transaction::getCount)
                    .filter(count -> count > 0)
                    .sum());
            row.put(CELL_COUNT, Math.abs(positions.getTransactions()
                    .stream()
                    .mapToInt(Transaction::getCount)
                    .filter(count -> count < 0)
                    .sum()) +
                    positions.getRedemptions()
                            .stream()
                            .mapToInt(SecurityEventCashFlow::getCount)
                            .sum());
            int count = getCount(positions);
            row.put(COUNT, count);
            if (count == 0) {
                row.put(GROSS_PROFIT, "=" + getGrossProfit(portfolio, security, positions) +
                        ((securityType == STOCK_OR_BOND) ? ("+" + AMORTIZATION.getCellAddr()) : ""));
            } else {
                row.put(AVERAGE_PRICE, getPurchaseCost(security, positions)
                        .abs()
                        .divide(BigDecimal.valueOf(Math.max(1, Math.abs(count))), 2, RoundingMode.CEILING));
                row.put(AVERAGE_ACCRUED_INTEREST, getPurchaseAccruedInterest(security, positions)
                        .abs()
                        .divide(BigDecimal.valueOf(Math.max(1, Math.abs(count))), 2, RoundingMode.CEILING));

                if (securityType == CURRENCY_PAIR) {
                    String currency = getCurrencyPair(security.getIsin()).substring(0, 3);
                    if (LocalDate.ofInstant(filter.getToDate(), ZoneId.systemDefault()).compareTo(LocalDate.now()) >= 0) {
                        row.put(LAST_PRICE, foreignExchangeRateService.getExchangeRateToRub(currency));
                    } else {
                        row.put(LAST_PRICE, foreignExchangeRateService.getExchangeRateToRub(
                                currency,
                                filter.getToDate(),
                                ZoneId.systemDefault()));
                    }
                } else {
                    securityQuoteRepository
                            .findFirstBySecurityIsinAndTimestampLessThanOrderByTimestampDesc(security.getIsin(), filter.getToDate())
                            .ifPresent(quote -> {
                                if (securityType == STOCK_OR_BOND) {
                                    row.put(LAST_PRICE, Optional.ofNullable(quote.getPrice()) // for bonds
                                            .orElse(quote.getQuote())); // for stocks
                                    row.put(LAST_ACCRUED_INTEREST, quote.getAccruedInterest());
                                } else if (securityType == DERIVATIVE) {
                                    row.put(LAST_PRICE, quote.getPrice());
                                }
                            });
                }

                if (securityType == STOCK_OR_BOND || securityType == CURRENCY_PAIR) {
                    row.put(GROSS_PROFIT, STOCK_GROSS_PROFIT_FORMULA);
                } else if (securityType == DERIVATIVE) {
                    row.put(GROSS_PROFIT, getGrossProfit(portfolio, security, positions));
                }
                if (securityType == STOCK_OR_BOND || securityType == CURRENCY_PAIR) {
                    row.put(INVESTMENT_PROPORTION, INVESTMENT_PROPORTION_FORMULA);
                    row.put(PROPORTION, PROPORTION_FORMULA);
                }
            }
            row.put(COMMISSION, getTotal(positions.getTransactions(), CashFlowType.COMMISSION).abs());
            if (securityType == STOCK_OR_BOND) {
                row.put(COUPON, sumPaymentsForType(portfolio, security, CashFlowType.COUPON));
                row.put(AMORTIZATION, sumPaymentsForType(portfolio, security, CashFlowType.AMORTIZATION));
                row.put(DIVIDEND, sumPaymentsForType(portfolio, security, CashFlowType.DIVIDEND));
                row.put(TAX, sumPaymentsForType(portfolio, security, CashFlowType.TAX).abs());
            }
            row.put(PROFIT, PROFIT_FORMULA);
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
        if (getSecurityType(security) == STOCK_OR_BOND) {
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

    /**
     * Возвращает последний известный остаток денежных средств соответствующей дате, не позже указанной.
     */
    private Optional<PortfolioProperty> getPortfolioCash(Portfolio portfolio, Instant atInstant) {
        return portfolioPropertyRepository
                .findFirstByPortfolioIdAndPropertyAndTimestampBetweenOrderByTimestampDesc(
                        portfolio.getId(),
                        PortfolioPropertyType.CASH.name(),
                        Instant.ofEpochSecond(0),
                        atInstant)
                .map(portfolioPropertyConverter::fromEntity);
    }

    private static String getStockOrBondGrossProfitFormula() {
        return "=IF(" + LAST_PRICE.getCellAddr() + "<>\"\",(" +
                LAST_PRICE.getCellAddr() + "+" + LAST_ACCRUED_INTEREST.getCellAddr() + "-" +
                AVERAGE_PRICE.getCellAddr() + "-" + AVERAGE_ACCRUED_INTEREST.getCellAddr() + ")*" +
                COUNT.getCellAddr() + "+" + AMORTIZATION.getCellAddr() + ",0)";
    }

    private static String getProfitFormula() {
        return "=" + COUPON.getCellAddr() + "+" + DIVIDEND.getCellAddr() + "+" + GROSS_PROFIT.getCellAddr() +
                "-" + TAX.getCellAddr() + "-" + COMMISSION.getCellAddr();
    }

    private static String getProfitProportionFormula() {
        return "=" + PROFIT.getCellAddr() + "/ABS(" + PROFIT.getColumnIndex() + "2)";
    }

    private static String getInvestmentProportionFormula() {
        return "=IF(" + COUNT.getCellAddr() + ">0,1,0)*" +
                "((" + AVERAGE_PRICE.getCellAddr() + "+" + AVERAGE_ACCRUED_INTEREST.getCellAddr() + ")*" + COUNT.getCellAddr() +
                "-" + AMORTIZATION.getCellAddr() + ")" +
                "/(SUMPRODUCT((0+" + AVERAGE_PRICE.getRange(3, 1000) + ")," +
                "(0+" + COUNT.getRange(3, 1000) + ")," +
                "SIGN(" + COUNT.getRange(3, 1000) + ">0)," +
                "(0+(" + TYPE.getRange(3, 1000) + "<>\"" + DERIVATIVE.getDescription() + "\")))" +
                "+SUMPRODUCT((0+" + AVERAGE_ACCRUED_INTEREST.getRange(3, 1000) + ")," +
                "(0+" + COUNT.getRange(3, 1000) + ")," +
                "SIGN(" + COUNT.getRange(3, 1000) + ">0))" +
                "-SUMIF(" + COUNT.getRange(3, 1000) + ",\">0\"," + AMORTIZATION.getRange(3, 1000) + "))";
    }

    private static String getProportionFormula() {
        return "=IF(" + COUNT.getCellAddr() + ">0,1,0)*" +
                "((" + LAST_PRICE.getCellAddr() + "+" + LAST_ACCRUED_INTEREST.getCellAddr() + ")*" + COUNT.getCellAddr() +
                ")/(SUMPRODUCT((0+" + LAST_PRICE.getRange(3, 1000) + ")," +
                "(0+" + COUNT.getRange(3, 1000) + ")," +
                "SIGN(" + COUNT.getRange(3, 1000) + ">0)," +
                "(0+(" + TYPE.getRange(3, 1000) + "<>\"" + DERIVATIVE.getDescription() + "\")))" +
                "+SUMPRODUCT((0+" + LAST_ACCRUED_INTEREST.getRange(3, 1000) + ")," +
                "(0+" + COUNT.getRange(3, 1000) + ")," +
                "SIGN(" + COUNT.getRange(3, 1000) + ">0)))";
    }
}

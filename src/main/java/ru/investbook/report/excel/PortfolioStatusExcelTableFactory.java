/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.report.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.pojo.Transaction;
import org.spacious_team.broker.report_parser.api.PortfolioCash;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioPropertyConverter;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.converter.SecurityQuoteConverter;
import ru.investbook.entity.PortfolioPropertyEntity;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.report.ClosedPosition;
import ru.investbook.report.FifoPositions;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.report.InternalRateOfReturn;
import ru.investbook.report.OpenedPosition;
import ru.investbook.report.PositionHistory;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.SecurityQuoteRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.spacious_team.broker.pojo.SecurityType.*;
import static ru.investbook.report.excel.PortfolioStatusExcelTableHeader.*;

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
    protected final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final SecurityQuoteRepository securityQuoteRepository;
    private final SecurityQuoteConverter securityQuoteConverter;
    private final SecurityConverter securityConverter;
    protected final PortfolioPropertyConverter portfolioPropertyConverter;
    private final FifoPositionsFactory positionsFactory;
    private final ForeignExchangeRateService foreignExchangeRateService;
    protected final PortfolioPropertyRepository portfolioPropertyRepository;
    private final InternalRateOfReturn internalRateOfReturn;
    private final Instant instantOf2000_01_01 = LocalDate.of(2000, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    private final Set<Integer> paymentEvents = Set.of(
            CashFlowType.AMORTIZATION.getId(),
            CashFlowType.REDEMPTION.getId(),
            CashFlowType.COUPON.getId(),
            CashFlowType.DIVIDEND.getId(),
            CashFlowType.DERIVATIVE_PROFIT.getId());

    public Table create(Portfolio portfolio) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Table create(String forCurrency) {
        return create(Optional.empty(), forCurrency);
    }

    public Table create(Portfolio portfolio, String forCurrency) {
        return create(Optional.of(portfolio), forCurrency);
    }

    private Table create(Optional<Portfolio> portfolio, String forCurrency) {
        Collection<String> securitiesId = getSecuritiesId(portfolio, forCurrency);
        Table table = create(portfolio, securitiesId, forCurrency);
        table.add(getCashRow(portfolio, forCurrency));
        return table;
    }

    private Table create(Optional<Portfolio> portfolio, Collection<String> securitiesIsin, String forCurrency) {
        Table table = new Table();
        for (String isin : securitiesIsin) {
            getSecurity(isin)
                    .map(security -> getSecurityStatus(portfolio, security, forCurrency))
                    .ifPresent(table::add);
        }
        return table;
    }

    private Collection<String> getSecuritiesId(Optional<Portfolio> portfolio, String currency) {
        Collection<String> contracts = new ArrayList<>();
        if (portfolio.isPresent()) {
            contracts.addAll(
                    transactionRepository.findDistinctSecurityByPortfolioAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
                            portfolio.get(),
                            currency,
                            ViewFilter.get().getFromDate(),
                            ViewFilter.get().getToDate()));
            contracts.addAll(
                    transactionRepository.findDistinctFxCurrencyPairByPortfolioAndCurrencyAndTimestampBetween(
                            portfolio.get(),
                            currency,
                            ViewFilter.get().getFromDate(),
                            ViewFilter.get().getToDate()));
            if (currency.equalsIgnoreCase("RUB")) {
                contracts.addAll(
                        transactionRepository.findDistinctDerivativeByPortfolioAndTimestampBetweenOrderByTimestampDesc(
                                portfolio.get(),
                                ViewFilter.get().getFromDate(),
                                ViewFilter.get().getToDate()));
            }
        } else {
            contracts.addAll(
                    transactionRepository.findDistinctSecurityByCurrencyAndTimestampBetweenOrderByTimestampDesc(
                            currency,
                            ViewFilter.get().getFromDate(),
                            ViewFilter.get().getToDate()));
            contracts.addAll(
                    transactionRepository.findDistinctFxCurrencyPairByCurrencyAndTimestampBetween(
                            currency,
                            ViewFilter.get().getFromDate(),
                            ViewFilter.get().getToDate()));
            if (currency.equalsIgnoreCase("RUB")) {
                contracts.addAll(
                        transactionRepository.findDistinctDerivativeByTimestampBetweenOrderByTimestampDesc(
                                ViewFilter.get().getFromDate(),
                                ViewFilter.get().getToDate()));
            }
        }
        return contracts;
    }

    protected Table.Record getCashRow(Optional<Portfolio> portfolio, String forCurrency) {
        Table.Record row = new Table.Record();
        Instant atTime = Instant.ofEpochSecond(Math.min(
                ViewFilter.get().getToDate().getEpochSecond(),
                Instant.now().getEpochSecond()));
        row.put(SECURITY, "Остаток денежных средств, " + forCurrency.toLowerCase());
        Collection<PortfolioProperty> portfolioCashes = getPortfolioCash(portfolio, atTime);
        row.put(LAST_EVENT_DATE, portfolioCashes.stream()
                .map(PortfolioProperty::getTimestamp)
                .reduce((t1, t2) -> t1.isAfter(t2) ? t1 : t2)
                .orElse(null));
        BigDecimal portfolioCash = portfolioCashes.stream()
                .map(portfolioProperty ->
                        PortfolioCash.valueOf(portfolioProperty.getValue())
                                .stream()
                                .filter(cash -> forCurrency.equals(cash.getCurrency()))
                                .map(PortfolioCash::getValue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private Optional<Security> getSecurity(String securityId) {
        if (getSecurityType(securityId) == CURRENCY_PAIR) {
            return Optional.of(Security.builder()
                    .id(SecurityType.getCurrencyPair(securityId))
                    .build());
        } else {
            return securityRepository.findById(securityId)
                    .map(securityConverter::fromEntity);
        }
    }

    private Table.Record getSecurityStatus(Optional<Portfolio> portfolio, Security security, String toCurrency) {
        Table.Record row = new Table.Record();
        SecurityType securityType = getSecurityType(security);
        row.put(SECURITY,
                Optional.ofNullable(security.getName())
                        .orElse((securityType == CURRENCY_PAIR) ?
                                getCurrencyPair(security.getId()) :
                                security.getId()));
        row.put(TYPE, securityType.getDescription());
        try {
            ViewFilter filter = ViewFilter.get();
            FifoPositions positions = positionsFactory.get(portfolio, security, filter);
            row.put(FIRST_TRANSACTION_DATE, Optional.ofNullable(positions.getPositionHistories().peekFirst())
                    .map(PositionHistory::getInstant)
                    .orElse(null));
            row.put(LAST_TRANSACTION_DATE, Optional.ofNullable(positions.getPositionHistories().peekLast())
                    .map(PositionHistory::getInstant)
                    .orElse(null));
            if (securityType != CURRENCY_PAIR) {
                row.put(LAST_EVENT_DATE, getLastEventDate(portfolio, security, filter)
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

            SecurityQuote securityQuote = null;
            String quoteCurrency = null;
            int count = positions.getCurrentOpenedPositionsCount();
            row.put(COUNT, count);
            if (count == 0) {
                row.put(GROSS_PROFIT, "=" + getGrossProfit(portfolio, security, positions, toCurrency) +
                        ((securityType == STOCK_OR_BOND) ? ("+" + AMORTIZATION.getCellAddr()) : ""));
            } else {
                row.put(AVERAGE_PRICE, getPurchaseCost(security, positions, toCurrency)
                        .abs()
                        .divide(BigDecimal.valueOf(Math.max(1, Math.abs(count))), 6, RoundingMode.CEILING));
                row.put(AVERAGE_ACCRUED_INTEREST, getPurchaseAccruedInterest(security, positions, toCurrency)
                        .abs()
                        .divide(BigDecimal.valueOf(Math.max(1, Math.abs(count))), 6, RoundingMode.CEILING));

                if (securityType == CURRENCY_PAIR) {
                    BigDecimal lastPrice;
                    String currencyPair = getCurrencyPair(security.getId());
                    String currency = currencyPair.substring(0, 3);
                    quoteCurrency = currencyPair.substring(3, 6);
                    LocalDate toDate = LocalDate.ofInstant(filter.getToDate(), ZoneId.systemDefault());
                    if (toDate.compareTo(LocalDate.now()) >= 0) {
                        lastPrice = foreignExchangeRateService.getExchangeRate(currency, quoteCurrency);
                    } else {
                        lastPrice = foreignExchangeRateService.getExchangeRateOrDefault(
                                currency,
                                quoteCurrency,
                                LocalDate.ofInstant(filter.getToDate(), ZoneId.systemDefault()));
                    }
                    row.put(LAST_PRICE, convertToCurrency(lastPrice, quoteCurrency, toCurrency));
                    securityQuote = SecurityQuote.builder()
                            .security(security.getId())
                            .timestamp(filter.getToDate())
                            .quote(lastPrice)
                            .build();
                } else {
                    Optional<SecurityQuote> optionalQuote = securityQuoteRepository
                            .findFirstBySecurityIdAndTimestampLessThanOrderByTimestampDesc(security.getId(), filter.getToDate())
                            .map(securityQuoteConverter::fromEntity);
                    optionalQuote.ifPresent(quote -> {
                        row.put(LAST_PRICE, quote.getCleanPriceInCurrency());
                        row.put(LAST_ACCRUED_INTEREST, quote.getAccruedInterest());
                    });
                    securityQuote = optionalQuote.orElse(null);
                    quoteCurrency = toCurrency; // TODO не известно точно в какой валюте котируется инструмент, делаем предположение, что в валюте сделки
                }

                if (securityType == STOCK_OR_BOND || securityType == CURRENCY_PAIR) {
                    row.put(GROSS_PROFIT, STOCK_GROSS_PROFIT_FORMULA);
                } else if (securityType == DERIVATIVE) {
                    row.put(GROSS_PROFIT, getGrossProfit(portfolio, security, positions, toCurrency));
                }
                if (securityType == STOCK_OR_BOND || securityType == CURRENCY_PAIR) {
                    row.put(INVESTMENT_PROPORTION, INVESTMENT_PROPORTION_FORMULA);
                    row.put(PROPORTION, PROPORTION_FORMULA);
                }
            }
            row.put(COMMISSION, getTotal(positions.getTransactions(), CashFlowType.COMMISSION, toCurrency).abs());
            if (securityType == STOCK_OR_BOND) {
                row.put(COUPON, sumPaymentsForType(portfolio, security, CashFlowType.COUPON, toCurrency));
                row.put(AMORTIZATION, sumPaymentsForType(portfolio, security, CashFlowType.AMORTIZATION, toCurrency));
                row.put(DIVIDEND, sumPaymentsForType(portfolio, security, CashFlowType.DIVIDEND, toCurrency));
                row.put(TAX, sumPaymentsForType(portfolio, security, CashFlowType.TAX, toCurrency).abs());
            }
            row.put(PROFIT, PROFIT_FORMULA);
            row.put(INTERNAL_RATE_OF_RETURN, internalRateOfReturn.calc(portfolio, security, securityQuote, quoteCurrency, filter));
            row.put(PROFIT_PROPORTION, PROFIT_PROPORTION_FORMULA);
        } catch (Exception e) {
            log.error("Ошибка при формировании агрегированных данных по бумаге {}", security, e);
        }
        return row;
    }

    private Optional<SecurityEventCashFlowEntity> getLastEventDate(Optional<Portfolio> portfolio, Security security, ViewFilter filter) {
        return portfolio
                .map(value ->
                        securityEventCashFlowRepository
                                .findFirstByPortfolioIdAndSecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
                                        value.getId(), security.getId(), paymentEvents, filter.getFromDate(), filter.getToDate()))
                .orElseGet(() ->
                        securityEventCashFlowRepository
                                .findFirstBySecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
                                        security.getId(), paymentEvents, filter.getFromDate(), filter.getToDate()));

    }

    /**
     * Курсовой доход с купли-продажи (для деривативов - суммарная вариационная маржа)
     */
    private BigDecimal getGrossProfit(Optional<Portfolio> portfolio, Security security, FifoPositions positions, String toCurrency) {
        SecurityType securityType = getSecurityType(security);
        return switch (securityType) {
            case STOCK_OR_BOND -> getPurchaseCost(security, positions, toCurrency)
                    .add(getPurchaseAccruedInterest(security, positions, toCurrency));
            case DERIVATIVE -> sumPaymentsForType(portfolio, security, CashFlowType.DERIVATIVE_PROFIT, toCurrency);
            case CURRENCY_PAIR -> getPurchaseCost(security, positions, toCurrency);
        };
    }

    /**
     * Разница доходов с продажи и расходов на покупку
     */
    private BigDecimal getPurchaseCost(Security security, FifoPositions positions, String toCurrency) {
        SecurityType securityType = getSecurityType(security);
        return switch (securityType) {
            case STOCK_OR_BOND -> getStockOrBondPurchaseCost(positions, toCurrency);
            case DERIVATIVE -> getTotal(positions.getTransactions(), CashFlowType.DERIVATIVE_PRICE, toCurrency);
            case CURRENCY_PAIR -> getTotal(positions.getTransactions(), CashFlowType.PRICE, toCurrency);
        };
    }

    /**
     * Разница цен продаж и покупок. Не учитывается цена покупки, если ЦБ выведена со счета, не учитывается цена
     * продажи, если ЦБ введена на счет
     */
    private BigDecimal getStockOrBondPurchaseCost(FifoPositions positions, String toCurrency) {
        BigDecimal purchaseCost = positions.getOpenedPositions()
                .stream()
                .map(openPosition -> getTransactionValue(openPosition.getOpenTransaction(), CashFlowType.PRICE, toCurrency)
                        .map(value -> value.multiply(getOpenAmountMultiplier(openPosition))))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add); // если ценная бумага не вводилась на счет, а была куплена (есть цена покупки)
        for (ClosedPosition closedPosition : positions.getClosedPositions()) {
            BigDecimal openPrice = getTransactionValue(closedPosition.getOpenTransaction(), CashFlowType.PRICE, toCurrency)
                    .map(value -> value.multiply(getOpenAmountMultiplier(closedPosition)))
                    .orElse(null);
            BigDecimal closePrice = getTransactionValue(closedPosition.getCloseTransaction(), CashFlowType.PRICE, toCurrency)
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
                .map(entity -> convertToCurrency(entity.getValue(), entity.getCurrency(), toCurrency))
                .map(BigDecimal::abs)
                .reduce(purchaseCost, BigDecimal::add);
    }

    /**
     * Разница проданного и купленного НКД
     */
    private BigDecimal getPurchaseAccruedInterest(Security security, FifoPositions positions, String toCurrency) {
        if (getSecurityType(security) == STOCK_OR_BOND) {
            return getTotal(positions.getTransactions(), CashFlowType.ACCRUED_INTEREST, toCurrency);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getTotal(Deque<Transaction> transactions, CashFlowType type, String toCurrency) {
        return transactions.stream()
                .filter(t -> t.getId() != null && t.getCount() != 0)
                .map(t -> getTransactionValue(t, type, toCurrency))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<BigDecimal> getTransactionValue(Transaction t, CashFlowType type, String toCurrency) {
        if (t.getId() == null) { // redemption
            return Optional.empty();
        }
        return transactionCashFlowRepository
                .findByPkPortfolioAndPkTransactionIdAndPkType(t.getPortfolio(), t.getId(), type.getId())
                .map(entity -> convertToCurrency(entity.getValue(), entity.getCurrency(), toCurrency));
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

    private BigDecimal sumPaymentsForType(Optional<Portfolio> portfolio, Security security, CashFlowType cashFlowType, String toCurrency) {
        return getSecurityEventCashFlowEntities(portfolio, security, cashFlowType)
                .stream()
                .map(entity -> convertToCurrency(entity.getValue(), entity.getCurrency(), toCurrency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<SecurityEventCashFlowEntity> getSecurityEventCashFlowEntities(Optional<Portfolio> portfolio,
                                                                                     Security security,
                                                                                     CashFlowType cashFlowType) {
        return portfolio
                .map(value ->
                        securityEventCashFlowRepository
                                .findByPortfolioIdAndSecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                                        value.getId(),
                                        security.getId(),
                                        cashFlowType.getId(),
                                        ViewFilter.get().getFromDate(),
                                        ViewFilter.get().getToDate()))
                .orElseGet(() ->
                        securityEventCashFlowRepository
                                .findBySecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                                        security.getId(),
                                        cashFlowType.getId(),
                                        ViewFilter.get().getFromDate(),
                                        ViewFilter.get().getToDate()));
    }

    /**
     * Возвращает для портфеля последний известный остаток денежных средств соответствующей дате, не позже указанной.
     * Если портфель не указан, возвращает для всех портфелей сумму последних известных остатков денежных средств
     * соответствующих дате, не позже указанной.
     */
    protected Collection<PortfolioProperty> getPortfolioCash(Optional<Portfolio> portfolio, Instant atInstant) {
        return portfolio
                .flatMap(value ->
                        portfolioPropertyRepository
                                .findFirstByPortfolioIdAndPropertyAndTimestampBetweenOrderByTimestampDesc(
                                        value.getId(),
                                        PortfolioPropertyType.CASH.name(),
                                        Instant.ofEpochSecond(0),
                                        atInstant))
                .map(value -> (Collection<PortfolioPropertyEntity>) Collections.singleton(value))
                .orElseGet(() ->
                        portfolioPropertyRepository
                                .findDistinctOnPortfolioByPropertyAndTimestampBetweenOrderByTimestampDesc(
                                        PortfolioPropertyType.CASH.name(),
                                        Instant.ofEpochSecond(0),
                                        atInstant))
                .stream()
                .map(portfolioPropertyConverter::fromEntity)
                    .collect(Collectors.toList());
    }

    private BigDecimal convertToCurrency(BigDecimal value, String fromCurrency, String toCurrency) {
        return foreignExchangeRateService.convertValueToCurrency(value, fromCurrency, toCurrency);
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

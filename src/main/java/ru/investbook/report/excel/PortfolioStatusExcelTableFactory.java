/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioPropertyConverter;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.report.FifoPositions;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.report.FifoPositionsFilter;
import ru.investbook.report.InternalRateOfReturn;
import ru.investbook.report.PositionHistory;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.service.SecurityProfitService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static org.spacious_team.broker.pojo.SecurityType.*;
import static ru.investbook.report.excel.PortfolioStatusExcelTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioStatusExcelTableFactory implements TableFactory {
    static final BigDecimal minCash = BigDecimal.valueOf(0.01);
    private static final String STOCK_OR_BOND_GROSS_PROFIT_FORMULA = getStockOrBondGrossProfitFormula();
    private static final String PROFIT_FORMULA = getProfitFormula();
    private static final String PROFIT_PROPORTION_FORMULA = getProfitProportionFormula();
    private static final String INVESTMENT_PROPORTION_FORMULA = getInvestmentProportionFormula();
    private static final String PROPORTION_FORMULA = getProportionFormula();
    protected final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final SecurityConverter securityConverter;
    protected final PortfolioPropertyConverter portfolioPropertyConverter;
    private final FifoPositionsFactory positionsFactory;
    private final SecurityProfitService securityProfitService;
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

    public Table create(Portfolio portfolio, String forCurrency) {
        return create(singleton(portfolio.getId()), forCurrency);
    }

    /**
     * @param portfolios should be empty for display for all
     */
    @Override
    public Table create(Collection<String> portfolios, String forCurrency) {
        Collection<String> securitiesId = getSecuritiesId(portfolios, forCurrency);
        Table table = create(portfolios, securitiesId, forCurrency);
        table.add(getCashRow(portfolios, forCurrency));
        return table;
    }

    private Table create(Collection<String> portfolios, Collection<String> securitiesIsin, String forCurrency) {
        Table table = new Table();
        for (String isin : securitiesIsin) {
            getSecurity(isin)
                    .map(security -> getSecurityStatus(portfolios, security, forCurrency))
                    .ifPresent(table::add);
        }
        return table;
    }

    private Collection<String> getSecuritiesId(Collection<String> portfolios, String currency) {
        Collection<String> contracts = new ArrayList<>();
        ViewFilter filter = ViewFilter.get();
        if (portfolios.isEmpty()) {
            contracts.addAll(
                    transactionRepository.findDistinctSecurityByCurrencyAndTimestampBetweenOrderByTimestampDesc(
                            currency,
                            filter.getFromDate(),
                            filter.getToDate()));
            contracts.addAll(
                    transactionRepository.findDistinctFxCurrencyPairByCurrencyAndTimestampBetween(
                            currency,
                            filter.getFromDate(),
                            filter.getToDate()));
            if (currency.equalsIgnoreCase("RUB")) {
                contracts.addAll(
                        transactionRepository.findDistinctDerivativeByTimestampBetweenOrderByTimestampDesc(
                                filter.getFromDate(),
                                filter.getToDate()));
            }
        } else {
            contracts.addAll(
                    transactionRepository.findDistinctSecurityByPortfolioInAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
                            portfolios,
                            currency,
                            filter.getFromDate(),
                            filter.getToDate()));
            contracts.addAll(
                    transactionRepository.findDistinctFxCurrencyPairByPortfolioInAndCurrencyAndTimestampBetween(
                            portfolios,
                            currency,
                            filter.getFromDate(),
                            filter.getToDate()));
            if (currency.equalsIgnoreCase("RUB")) {
                contracts.addAll(
                        transactionRepository.findDistinctDerivativeByPortfolioInAndTimestampBetweenOrderByTimestampDesc(
                                portfolios,
                                filter.getFromDate(),
                                filter.getToDate()));
            }
        }
        return contracts;
    }

    protected Table.Record getCashRow(Collection<String> portfolios, String forCurrency) {
        Table.Record row = new Table.Record();
        Instant atTime = Instant.ofEpochSecond(Math.min(
                ViewFilter.get().getToDate().getEpochSecond(),
                Instant.now().getEpochSecond()));
        row.put(SECURITY, "Остаток денежных средств, " + forCurrency.toLowerCase());
        Collection<PortfolioProperty> portfolioCashes = securityProfitService.getPortfolioCash(portfolios, atTime);
        row.put(LAST_EVENT_DATE, portfolioCashes.stream()
                .map(PortfolioProperty::getTimestamp)
                .reduce((t1, t2) -> t1.isAfter(t2) ? t1 : t2)
                .orElse(null));
        BigDecimal portfolioCash = portfolioCashes.stream()
                .map(portfolioProperty -> {
                    try {
                        return PortfolioCash.deserialize(portfolioProperty.getValue())
                                .stream()
                                .filter(cash -> forCurrency.equals(cash.getCurrency()))
                                .map(PortfolioCash::getValue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                    } catch (Exception e) {
                        log.warn("Ошибка при десериализации свойства: {}", portfolioProperty.getValue(), e);
                        return BigDecimal.ZERO;
                    }
                })
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
        return securityRepository.findById(securityId)
                .map(securityConverter::fromEntity);
    }

    private Table.Record getSecurityStatus(Collection<String> portfolios, Security security, String toCurrency) {
        Table.Record row = new Table.Record();
        SecurityType securityType = security.getType();
        row.put(SECURITY,
                ofNullable(security.getName())
                        .or(() -> ofNullable(security.getTicker()))
                        .orElse((securityType == CURRENCY_PAIR) ?
                                getCurrencyPair(security.getId()) :
                                security.getId()));
        row.put(TYPE, securityType.getDescription());
        try {
            ViewFilter filter = ViewFilter.get();
            FifoPositionsFilter pf = FifoPositionsFilter.of(portfolios, filter.getFromDate(), filter.getToDate());
            FifoPositions positions = positionsFactory.get(security, pf);
            row.put(FIRST_TRANSACTION_DATE, Optional.ofNullable(positions.getPositionHistories().peekFirst())
                    .map(PositionHistory::getInstant)
                    .orElse(null));
            row.put(LAST_TRANSACTION_DATE, Optional.ofNullable(positions.getPositionHistories().peekLast())
                    .map(PositionHistory::getInstant)
                    .orElse(null));
            if (securityType != CURRENCY_PAIR) {
                row.put(LAST_EVENT_DATE,
                        securityProfitService.getLastEventTimestamp(
                                        portfolios, security, paymentEvents, filter.getFromDate(), filter.getToDate())
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

            SecurityQuote quote = null;
            int count = positions.getCurrentOpenedPositionsCount();
            row.put(COUNT, count);
            if (count == 0) {
                row.put(GROSS_PROFIT, "=" + securityProfitService.getGrossProfit(portfolios, security, positions, toCurrency) +
                        (securityType.isBond() ? ("+" + AMORTIZATION.getCellAddr()) : ""));
            } else {
                row.put(AVERAGE_PRICE, securityProfitService.getPurchaseCost(security, positions, toCurrency)
                        .abs()
                        .divide(BigDecimal.valueOf(Math.max(1, Math.abs(count))), 6, RoundingMode.CEILING));
                row.put(AVERAGE_ACCRUED_INTEREST, securityProfitService.getPurchaseAccruedInterest(security, positions, toCurrency)
                        .abs()
                        .divide(BigDecimal.valueOf(Math.max(1, Math.abs(count))), 6, RoundingMode.CEILING));

                quote = securityProfitService.getSecurityQuote(security, toCurrency, filter.getToDate());

                if (quote != null) {
                    row.put(LAST_PRICE, quote.getCleanPriceInCurrency(securityType == DERIVATIVE));
                    row.put(LAST_ACCRUED_INTEREST, quote.getAccruedInterest());
                }

                if (securityType == DERIVATIVE) {
                    row.put(GROSS_PROFIT, securityProfitService.getGrossProfit(portfolios, security, positions, toCurrency));
                } else {
                    row.put(GROSS_PROFIT, STOCK_OR_BOND_GROSS_PROFIT_FORMULA);
                }
                if (securityType != DERIVATIVE && securityType != CURRENCY_PAIR) {
                    row.put(INVESTMENT_PROPORTION, INVESTMENT_PROPORTION_FORMULA);
                    row.put(PROPORTION, PROPORTION_FORMULA);
                }
            }
            row.put(COMMISSION, securityProfitService.getTotal(positions.getTransactions(), CashFlowType.COMMISSION, toCurrency).abs());
            if (securityType.isBond()) {
                row.put(COUPON, securityProfitService.sumPaymentsForType(portfolios, security, CashFlowType.COUPON, toCurrency));
                row.put(AMORTIZATION, securityProfitService.sumPaymentsForType(portfolios, security, CashFlowType.AMORTIZATION, toCurrency));
            }
            if (securityType.isStock()) {
                row.put(DIVIDEND, securityProfitService.sumPaymentsForType(portfolios, security, CashFlowType.DIVIDEND, toCurrency));
            }
            if (securityType != DERIVATIVE && securityType != CURRENCY_PAIR) {
                row.put(TAX, securityProfitService.sumPaymentsForType(portfolios, security, CashFlowType.TAX, toCurrency).abs());
            }
            row.put(PROFIT, PROFIT_FORMULA);
            row.put(INTERNAL_RATE_OF_RETURN, internalRateOfReturn.calc(
                    portfolios, security, quote, filter.getFromDate(), filter.getToDate()));
            row.put(PROFIT_PROPORTION, PROFIT_PROPORTION_FORMULA);
        } catch (Exception e) {
            log.error("Ошибка при формировании агрегированных данных по бумаге {}", security, e);
        }
        return row;
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

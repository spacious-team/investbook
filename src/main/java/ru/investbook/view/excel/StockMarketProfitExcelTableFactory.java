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

package ru.investbook.view.excel;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.stereotype.Component;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.view.ClosedPosition;
import ru.investbook.view.FifoPositions;
import ru.investbook.view.FifoPositionsFactory;
import ru.investbook.view.ForeignExchangeRateService;
import ru.investbook.view.OpenedPosition;
import ru.investbook.view.PaidInterest;
import ru.investbook.view.PaidInterestFactory;
import ru.investbook.view.Position;
import ru.investbook.view.Table;
import ru.investbook.view.TableFactory;
import ru.investbook.view.ViewFilter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static ru.investbook.view.excel.StockMarketProfitExcelTableHeader.*;

@Component
@RequiredArgsConstructor
public class StockMarketProfitExcelTableFactory implements TableFactory {
    private static final String TAX_LIABILITY_FORMULA = getTaxLiabilityFormula();
    // isin -> security price currency
    private final Map<String, String> securityCurrencies = new ConcurrentHashMap<>();
    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final SecurityConverter securityConverter;
    private final PaidInterestFactory paidInterestFactory;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final ForeignExchangeRateService foreignExchangeRateService;
    private final FifoPositionsFactory positionsFactory;

    public Table create(Portfolio portfolio) {
        throw new UnsupportedOperationException();
    }

    public Table create(Portfolio portfolio, String forCurrency) {
        Collection<String> securitiesIsin = getSecuritiesIsin(portfolio, forCurrency);
        return create(portfolio, securitiesIsin);
    }

    public Table create(Portfolio portfolio, Collection<String> securitiesIsin) {
        Table openPositionsProfit = new Table();
        Table closedPositionsProfit = new Table();
        for (String isin : securitiesIsin) {
            securityRepository.findById(isin)
                    .map(securityConverter::fromEntity)
                    .ifPresent(security ->
                            getRowsForSecurity(security, portfolio, openPositionsProfit, closedPositionsProfit));
        }
        Table profit = new Table();
        profit.addAll(openPositionsProfit);
        profit.addAll(closedPositionsProfit);
        return profit;
    }

    private Collection<String> getSecuritiesIsin(Portfolio portfolio, String currency) {
        return transactionRepository.findDistinctSecurityByPortfolioAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
                portfolio,
                currency,
                ViewFilter.get().getFromDate(),
                ViewFilter.get().getToDate());
    }

    private void getRowsForSecurity(Security security, Portfolio portfolio, Table openPositionsProfit, Table closedPositionsProfit) {
        FifoPositions positions = positionsFactory.get(portfolio, security, ViewFilter.get());
        PaidInterest paidInterest = paidInterestFactory.get(portfolio, security, ViewFilter.get());
        openPositionsProfit.addAll(getPositionProfit(security, positions.getOpenedPositions(),
                paidInterest, this::getOpenedPositionProfit));
        closedPositionsProfit.addAll(getPositionProfit(security, positions.getClosedPositions(),
                paidInterest, this::getClosedPositionProfit));
        openPositionsProfit.addAll(getPositionProfit(security, paidInterest.getFictitiousPositions(),
                paidInterest, this::getOpenedPositionProfit));
    }

    private <T extends Position> Table getPositionProfit(Security security,
                                                         Deque<T> positions,
                                                         PaidInterest paidInterest,
                                                         Function<T, Table.Record> profitBuilder) {
        Table rows = new Table();
        for (T position : positions) {
            Table.Record record = profitBuilder.apply(position);
            record.putAll(getPaidInterestProfit(position, paidInterest));
            record.put(SECURITY,
                    ofNullable(security.getName())
                            .or(() -> ofNullable(security.getTicker()))
                            .orElse(security.getId()));
            rows.add(record);
        }
        return rows;
    }

    private Table.Record getOpenedPositionProfit(OpenedPosition position) {
        Table.Record row = new Table.Record();
        Transaction transaction = position.getOpenTransaction();
        row.put(OPEN_DATE, transaction.getTimestamp());
        row.put(COUNT, Math.abs(position.getCount()) * Integer.signum(transaction.getCount()));
        BigDecimal openPrice = getTransactionCashFlow(transaction, CashFlowType.PRICE, 1d / transaction.getCount());
        if (openPrice == null && (position instanceof ClosedPosition)) {
            // ЦБ введены, а не куплены, принимаем цену покупки = цене продажи, чтобы не было финфнсового результата
            Transaction closeTransaction = ((ClosedPosition) position).getCloseTransaction();
            openPrice = getTransactionCashFlow(closeTransaction, CashFlowType.PRICE, 1d / closeTransaction.getCount());
        }
        row.put(OPEN_PRICE, openPrice);
        if (openPrice != null) {
            row.put(OPEN_AMOUNT, openPrice.multiply(BigDecimal.valueOf(position.getCount())));
        }
        double multiplier = Math.abs(1d * position.getCount() / transaction.getCount());
        row.put(OPEN_ACCRUED_INTEREST, getTransactionCashFlow(transaction, CashFlowType.ACCRUED_INTEREST, multiplier));
        row.put(OPEN_COMMISSION, getTransactionCashFlow(transaction, CashFlowType.COMMISSION, multiplier));
        return row;
    }

    private Table.Record getClosedPositionProfit(ClosedPosition position) {
        // open transaction info
        Table.Record row = new Table.Record(getOpenedPositionProfit(position));
        // close transaction info
        Transaction transaction = position.getCloseTransaction();
        double multiplier = Math.abs(1d * position.getCount() / transaction.getCount());
        row.put(CLOSE_DATE, transaction.getTimestamp());
        BigDecimal closeAmount = switch (position.getClosingEvent()) {
            case PRICE -> getTransactionCashFlow(transaction, CashFlowType.PRICE, multiplier);
            case REDEMPTION -> getRedemptionCashFlow(transaction.getPortfolio(), transaction.getSecurity(), multiplier);
            default -> throw new IllegalArgumentException("ЦБ " + transaction.getSecurity() +
                    " не может быть закрыта событием типа " + position.getClosingEvent());
        };
        if (closeAmount == null) {
            // ЦБ выведены со счета, а не прданы, принимаем цену продажи = цене покупки, чтобы не было фин. результата
            closeAmount = getTransactionCashFlow(position.getOpenTransaction(), CashFlowType.PRICE, multiplier);
        }
        row.put(CLOSE_AMOUNT, closeAmount);
        row.put(CLOSE_ACCRUED_INTEREST, getTransactionCashFlow(transaction, CashFlowType.ACCRUED_INTEREST, multiplier));
        row.put(CLOSE_COMMISSION, getTransactionCashFlow(transaction, CashFlowType.COMMISSION, multiplier));
        boolean isLongPosition = isLongPosition(position);
        row.put(FORECAST_TAX, getForecastTax(isLongPosition));
        row.put(PROFIT, getClosedPositionProfit(isLongPosition));
        row.put(YIELD, getClosedPositionYield(isLongPosition));
        return row;
    }

    private boolean isLongPosition(ClosedPosition position) {
        return position.getOpenTransaction().getCount() > 0;
    }

    private Table.Record getPaidInterestProfit(Position position, PaidInterest paidInterest) {
        Table.Record info = new Table.Record();
        String coupon = convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.COUPON, position));
        String amortization = convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.AMORTIZATION, position));
        String dividend = convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.DIVIDEND, position));
        String tax = convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.TAX, position));
        info.put(COUPON, coupon);
        info.put(AMORTIZATION, amortization);
        info.put(DIVIDEND, dividend);
        info.put(TAX, tax);
        if (coupon != null || dividend != null) {
            if (paidInterest.getCurrency()
                    .filter("RUB"::equals)
                    .isEmpty()) {
                // считаем, что подписан W-8BEN, 10% налога удержаны иностранным эмитентом, нужно доплатить 3%
                info.put(TAX_LIABILITY, TAX_LIABILITY_FORMULA);
            }
        }
        return info;
    }

    private BigDecimal getTransactionCashFlow(Transaction transaction, CashFlowType type, double multiplier) {
        if (transaction.getId() == null) {
            return null;
        }
        return transactionCashFlowRepository
                .findByPkPortfolioAndPkTransactionIdAndPkType(
                        transaction.getPortfolio(),
                        transaction.getId(),
                        type.getId())
                .map(cash -> cash.getValue()
                        .multiply(exchangeRateToSecurityCurrency(cash.getCurrency(), transaction))
                        .multiply(BigDecimal.valueOf(multiplier))
                        .abs()
                        .setScale(6, RoundingMode.HALF_UP))
                .orElse(null);
    }

    private BigDecimal getRedemptionCashFlow(String portfolio, String isin, double multiplier) {
        List<SecurityEventCashFlowEntity> cashFlows = securityEventCashFlowRepository
                .findByPortfolioIdAndSecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                        portfolio,
                        isin,
                        CashFlowType.REDEMPTION.getId(),
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate());
        if (cashFlows.isEmpty()) {
            return null;
        } else if (cashFlows.size() != 1) {
            throw new IllegalArgumentException("По ЦБ может быть не более одного события погашения, по бумаге " + isin +
                    " найдено " + cashFlows.size() + " событий погашения: " + cashFlows);
        }
        return cashFlows.get(0)
                .getValue()
                .multiply(BigDecimal.valueOf(multiplier))
                .abs()
                .setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal exchangeRateToSecurityCurrency(String currency, Transaction transaction) {
        String securityCurrency = getSecurityCurrency(transaction);
        return currency.equalsIgnoreCase(securityCurrency) ?
                BigDecimal.ONE :
                foreignExchangeRateService.getExchangeRate(currency, securityCurrency);
    }

    /**
     * @return transaction price currency
     */
    private String getSecurityCurrency(Transaction transaction) {
        return securityCurrencies.computeIfAbsent(transaction.getSecurity(), $ -> getTransactionCurrency(transaction));
    }

    private String getTransactionCurrency(Transaction transaction) {
        return transactionCashFlowRepository
                .findByPkPortfolioAndPkTransactionIdAndPkType(
                        transaction.getPortfolio(),
                        transaction.getId(),
                        CashFlowType.PRICE.getId())
                .map(TransactionCashFlowEntity::getCurrency)
                .orElseThrow();
    }

    public static String convertPaidInterestToExcelFormula(List<SecurityEventCashFlow> pays) {
        if (pays == null || pays.isEmpty()) {
            return null;
        }
        return pays.stream()
                .map(SecurityEventCashFlow::getValue)
                .map(BigDecimal::abs)
                .map(String::valueOf)
                .collect(Collectors.joining("+", "=", ""));
    }

    private String getForecastTax(boolean isLongPosition) {
        String open = "(" + OPEN_AMOUNT.getCellAddr() + "+" + OPEN_ACCRUED_INTEREST.getCellAddr() + ")";
        String close = "(" + CLOSE_AMOUNT.getCellAddr() + "+" + CLOSE_ACCRUED_INTEREST.getCellAddr() + ")";
        String commission = "(" + OPEN_COMMISSION.getCellAddr() + "+" + CLOSE_COMMISSION.getCellAddr() + ")";
        String buy = isLongPosition ? open : close;
        String cell = isLongPosition ? close : open;
        String forecastTaxFormula = cell + "+" + AMORTIZATION.getCellAddr() + "-" + buy + "-" + commission;
        return "=IF(" + forecastTaxFormula + "<0,0,0.13*(" + forecastTaxFormula + "))";
    }

    private static String getTaxLiabilityFormula() {
        String payments = "(" + COUPON.getCellAddr() + "+" + DIVIDEND.getCellAddr() + ")";
        String tax = TAX.getCellAddr();
        return "=MAX(0,(0.13*" + payments + "-" + tax + "))";
    }

    private String getClosedPositionProfit(boolean isLongPosition) {
        String open = "(" + OPEN_AMOUNT.getCellAddr() + "+" + OPEN_ACCRUED_INTEREST.getCellAddr() + ")";
        String close = "(" + CLOSE_AMOUNT.getCellAddr() + "+" + CLOSE_ACCRUED_INTEREST.getCellAddr() + ")";
        String openCommission = OPEN_COMMISSION.getCellAddr();
        String closeCommission = CLOSE_COMMISSION.getCellAddr();
        String commission = "(" + openCommission + "+" + closeCommission + ")";
        String payments = "(" + COUPON.getCellAddr() + "+" + AMORTIZATION.getCellAddr() + "+" + DIVIDEND.getCellAddr() + ")";
        String tax = "(" + TAX.getCellAddr() + "+" + TAX_LIABILITY.getCellAddr() + "+" + FORECAST_TAX.getCellAddr() + ")";

        String buy = isLongPosition ? open : close;
        String cell = isLongPosition ? close : open;

        return "=" + cell + "+" + payments + "-" + buy + "-" + tax + "-" + commission;
    }

    private String getClosedPositionYield(boolean isLongPosition) {
        String profit = getClosedPositionProfit(isLongPosition).replace("=", "");
        String open = "(" + OPEN_AMOUNT.getCellAddr() + "+" + OPEN_ACCRUED_INTEREST.getCellAddr() + ")";
        String openCommission = OPEN_COMMISSION.getCellAddr();
        // TODO DAYS() excel function not impl by Apache POI: https://bz.apache.org/bugzilla/show_bug.cgi?id=58468
        String multiplier = "100*365/(1+ABS(DAYS360(" + OPEN_DATE.getCellAddr() + "," + CLOSE_DATE.getCellAddr() + ")))";
        return "=(" + profit + ")" +
                "/(" + open + "+" + openCommission + ")" +
                "*" + multiplier;
    }
}

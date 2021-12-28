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
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.stereotype.Component;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.report.ClosedPosition;
import ru.investbook.report.FifoPositions;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.report.FifoPositionsFilter;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.report.OpenedPosition;
import ru.investbook.report.PaidInterest;
import ru.investbook.report.PaidInterestFactory;
import ru.investbook.report.Position;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static ru.investbook.report.ForeignExchangeRateService.RUB;
import static ru.investbook.report.excel.StockMarketProfitExcelTableHeader.*;

@Component
@RequiredArgsConstructor
public class StockMarketProfitExcelTableFactory implements TableFactory {
    private static final String TAX_LIABILITY_FORMULA = getTaxLiabilityFormula();
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
        Collection<Security> securities = getSecurities(portfolio, forCurrency);
        return create(portfolio, securities, forCurrency);
    }

    private Collection<Security> getSecurities(Portfolio portfolio, String currency) {
        return transactionRepository.findDistinctSecurityByPortfolioInAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
                        singleton(portfolio.getId()),
                        currency,
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate())
                .stream()
                .map(securityRepository::findById)
                .flatMap(Optional::stream)
                .map(securityConverter::fromEntity)
                .toList();
    }

    public Table create(Portfolio portfolio, Collection<Security> securities, String toCurrency) {
        Table openPositionsProfit = new Table();
        Table closedPositionsProfit = new Table();
        securities.forEach(security ->
                getRowsForSecurity(security, portfolio, openPositionsProfit, closedPositionsProfit, toCurrency));
        Table profit = new Table();
        profit.addAll(openPositionsProfit);
        profit.addAll(closedPositionsProfit);
        return profit;
    }

    private void getRowsForSecurity(Security security, Portfolio portfolio, Table openPositionsProfit,
                                    Table closedPositionsProfit, String toCurrency) {
        ViewFilter filter = ViewFilter.get();
        FifoPositionsFilter pf = FifoPositionsFilter.of(portfolio, filter.getFromDate(), filter.getToDate());
        FifoPositions positions = positionsFactory.get(security, pf);
        PaidInterest paidInterest = paidInterestFactory.get(portfolio, security, filter.getFromDate(), filter.getToDate());
        openPositionsProfit.addAll(getPositionProfit(security, positions.getOpenedPositions(),
                paidInterest, this::getOpenedPositionProfit, toCurrency));
        closedPositionsProfit.addAll(getPositionProfit(security, positions.getClosedPositions(),
                paidInterest, this::getClosedPositionProfit, toCurrency));
        openPositionsProfit.addAll(getPositionProfit(security, paidInterest.getFictitiousPositions(),
                paidInterest, this::getOpenedPositionProfit, toCurrency));
    }

    private <T extends OpenedPosition> Table getPositionProfit(Security security,
                                                               Deque<T> positions,
                                                               PaidInterest paidInterest,
                                                               BiFunction<T, String, Table.Record> profitBuilder,
                                                               String toCurrency) {
        Table rows = new Table();
        for (T position : positions) {
            String openTransactionCurrency = getTransactionCurrency(position.getOpenTransaction(), toCurrency);
            if (openTransactionCurrency.equalsIgnoreCase(toCurrency)) {
                Table.Record record = profitBuilder.apply(position, toCurrency);
                record.putAll(getPaidInterestProfit(position, paidInterest, toCurrency));
                record.put(SECURITY,
                        ofNullable(security.getName())
                                .or(() -> ofNullable(security.getTicker()))
                                .or(() -> ofNullable(security.getIsin()))
                                .orElse("<неизвестно>"));
                rows.add(record);
            }
        }
        return rows;
    }

    private <T extends OpenedPosition> Table.Record getOpenedPositionProfit(T position, String toCurrency) {
        Table.Record row = new Table.Record();
        Transaction transaction = position.getOpenTransaction();
        row.put(OPEN_DATE, transaction.getTimestamp());
        row.put(COUNT, Math.abs(position.getCount()) * Integer.signum(transaction.getCount()));
        String openPrice = getTransactionCashFlow(transaction, CashFlowType.PRICE, 1d / transaction.getCount(), toCurrency);
        if (openPrice == null && (position instanceof ClosedPosition)) {
            // ЦБ введены, а не куплены, принимаем цену покупки = цене продажи, чтобы не было финансового результата
            Transaction closeTransaction = ((ClosedPosition) position).getCloseTransaction();
            openPrice = getTransactionCashFlow(closeTransaction, CashFlowType.PRICE, 1d / closeTransaction.getCount(), toCurrency);
        }
        row.put(OPEN_PRICE, openPrice);
        if (openPrice != null) {
            row.put(OPEN_AMOUNT, "=ABS(" + OPEN_PRICE.getCellAddr() + "*" + COUNT.getCellAddr() + ")");
        }
        double multiplier = Math.abs(1d * position.getCount() / transaction.getCount());
        row.put(OPEN_ACCRUED_INTEREST, getTransactionCashFlow(transaction, CashFlowType.ACCRUED_INTEREST, multiplier, toCurrency));
        row.put(OPEN_COMMISSION, getTransactionCashFlow(transaction, CashFlowType.COMMISSION, multiplier, toCurrency));
        return row;
    }

    private Table.Record getClosedPositionProfit(ClosedPosition position, String toCurrency) {
        // open transaction info
        Table.Record row = new Table.Record(getOpenedPositionProfit(position, toCurrency));
        // close transaction info
        Transaction transaction = position.getCloseTransaction();
        double multiplier = Math.abs(1d * position.getCount() / transaction.getCount());
        row.put(CLOSE_DATE, transaction.getTimestamp());
        String closeAmount = switch (position.getClosingEvent()) {
            case PRICE -> getTransactionCashFlow(transaction, CashFlowType.PRICE, multiplier, toCurrency);
            case REDEMPTION -> getRedemptionCashFlow(transaction.getPortfolio(), transaction.getSecurity(), multiplier, toCurrency);
            default -> throw new IllegalArgumentException("ЦБ " + transaction.getSecurity() +
                    " не может быть закрыта событием типа " + position.getClosingEvent());
        };
        if (closeAmount == null) {
            // ЦБ выведены со счета, а не проданы, принимаем цену продажи = цене покупки, чтобы не было финансового результата
            double withdrawalMultiplier = Math.abs(1d * position.getCount() / position.getOpenTransaction().getCount());
            closeAmount = getTransactionCashFlow(position.getOpenTransaction(), CashFlowType.PRICE, withdrawalMultiplier, toCurrency);
        }
        row.put(CLOSE_AMOUNT, closeAmount);
        row.put(CLOSE_ACCRUED_INTEREST, getTransactionCashFlow(transaction, CashFlowType.ACCRUED_INTEREST, multiplier, toCurrency));
        row.put(CLOSE_COMMISSION, getTransactionCashFlow(transaction, CashFlowType.COMMISSION, multiplier, toCurrency));
        boolean isLongPosition = isLongPosition(position);
        row.put(FORECAST_TAX, getForecastTax(isLongPosition));
        row.put(PROFIT, getClosedPositionProfit(isLongPosition));
        row.put(YIELD, getClosedPositionYield(isLongPosition));
        return row;
    }

    private boolean isLongPosition(ClosedPosition position) {
        return position.getOpenTransaction().getCount() > 0;
    }

    private Table.Record getPaidInterestProfit(Position position, PaidInterest paidInterest, String toCurrency) {
        Table.Record info = new Table.Record();
        info.put(COUPON, convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.COUPON, position), toCurrency));
        info.put(AMORTIZATION, convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.AMORTIZATION, position), toCurrency));
        info.put(DIVIDEND, convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.DIVIDEND, position), toCurrency));
        info.put(TAX, convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.TAX, position), toCurrency));
        if (!toCurrency.equals(RUB) || !paidInterest.getCurrencies().stream().allMatch(RUB::equals)) {
            // Если речь о сделках в иностранной валюте или хотя бы одна выплата была в иностранной валюте,
            // скорее всего нужно доплачивать в налоговую
            info.put(TAX_LIABILITY, TAX_LIABILITY_FORMULA);
        }
        return info;
    }

    private String getTransactionCashFlow(Transaction transaction, CashFlowType type, double multiplier, String toCurrency) {
        if (PaidInterest.isFictitiousPositionTransaction(transaction)) {
            return null;
        }
        return transactionCashFlowRepository
                .findByTransactionIdAndCashFlowType(transaction.getId(), type)
                .map(cash -> {
                    BigDecimal value = cash.getValue()
                            .multiply(BigDecimal.valueOf(multiplier))
                            .abs()
                            .setScale(6, RoundingMode.HALF_UP);
                    return "=" + convertValueToCurrencyFormula(value, cash.getCurrency(), toCurrency);
                })
                .orElse(null);
    }

    private String getRedemptionCashFlow(String portfolio, Integer securityId, double multiplier, String toCurrency) {
        List<SecurityEventCashFlowEntity> cashFlows = securityEventCashFlowRepository
                .findByPortfolioIdInAndSecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                        singleton(portfolio),
                        securityId,
                        CashFlowType.REDEMPTION.getId(),
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate());
        if (cashFlows.isEmpty()) {
            return null;
        } else if (cashFlows.size() > 1) {
            throw new IllegalArgumentException("По ЦБ может быть не более одного события погашения, по бумаге " + securityId +
                    " найдено " + cashFlows.size() + " событий погашения");
        }
        SecurityEventCashFlowEntity redemptionEntity = cashFlows.get(0);
        BigDecimal redemption = redemptionEntity.getValue()
                .multiply(BigDecimal.valueOf(multiplier))
                .abs()
                .setScale(6, RoundingMode.HALF_UP);
        return "=" + convertValueToCurrencyFormula(redemption, redemptionEntity.getCurrency(), toCurrency);
    }

    /**
     * @return transaction {@link CashFlowType#PRICE} currency
     */
    private String getTransactionCurrency(Transaction transaction, String fallbackCurrency) {
        if (PaidInterest.isFictitiousPositionTransaction(transaction)) {
            return fallbackCurrency;
        }
        return transactionCashFlowRepository
                .findByTransactionIdAndCashFlowType(transaction.getId(), CashFlowType.PRICE)
                .map(TransactionCashFlowEntity::getCurrency)
                .orElse(fallbackCurrency);
    }

    public String convertPaidInterestToExcelFormula(List<SecurityEventCashFlow> pays, String toCurrency) {
        if (pays == null || pays.isEmpty()) {
            return null;
        }
        return pays.stream()
                .map(cash -> convertValueToCurrencyFormula(cash.getValue().abs(), cash.getCurrency(), toCurrency))
                .collect(Collectors.joining("+", "=", ""));
    }

    private String convertValueToCurrencyFormula(BigDecimal value, String valueCurrency, String toCurrency) {
        if (valueCurrency.equals(toCurrency)) {
            return String.valueOf(value);
        } else {
            return value + "*" + foreignExchangeRateService.getExchangeRate(valueCurrency, toCurrency);
        }
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
        String profit = getClosedPositionProfit(isLongPosition).substring(1); // remove leading '='
        String open = "(" + OPEN_AMOUNT.getCellAddr() + "+" + OPEN_ACCRUED_INTEREST.getCellAddr() + ")";
        String openCommission = OPEN_COMMISSION.getCellAddr();
        // TODO DAYS() excel function not impl by Apache POI: https://bz.apache.org/bugzilla/show_bug.cgi?id=58468
        String multiplier = "100*365/(1+ABS(DAYS360(" + OPEN_DATE.getCellAddr() + "," + CLOSE_DATE.getCellAddr() + ")))";
        return "=(" + profit + ")" +
                "/(" + open + "+" + openCommission + ")" +
                "*" + multiplier;
    }
}

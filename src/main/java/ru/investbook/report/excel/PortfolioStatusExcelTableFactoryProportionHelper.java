/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

import ru.investbook.report.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.spacious_team.broker.pojo.SecurityType.CURRENCY_PAIR;
import static org.spacious_team.broker.pojo.SecurityType.DERIVATIVE;
import static ru.investbook.report.excel.PortfolioStatusExcelTableFactory.CASH_BALANCE;
import static ru.investbook.report.excel.PortfolioStatusExcelTableHeader.*;

public class PortfolioStatusExcelTableFactoryProportionHelper {

    static void setInvestmentProportionFormula(Table table) {
        BigDecimal totalInvestmentAmount = getRecordStreamForInvestmentAmount(table)
                .map(PortfolioStatusExcelTableFactoryProportionHelper::getInvestmentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalInvestmentAmount.floatValue() >= 0.01) {
            getRecordStreamForInvestmentAmount(table).forEach(record ->
                    record.put(INVESTMENT_PROPORTION, "=" + getInvestmentAmount(record) + "/" + totalInvestmentAmount));
        }
    }

    static void setCurrentProportionFormula(Table table) {
        BigDecimal totalAmount = getRecordStreamForTotalAmount(table)
                .map(PortfolioStatusExcelTableFactoryProportionHelper::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAmount.floatValue() >= 0.01) {
            getRecordStreamForTotalAmount(table).forEach(record ->
                    record.put(PROPORTION, "=" + getCurrentAmount(record) + "/" + totalAmount));
        }
    }

    private static Stream<Table.Record> getRecordStreamForInvestmentAmount(Table table) {
        return table.stream()
                .filter(PortfolioStatusExcelTableFactoryProportionHelper::hasPositiveCount)
                .filter(Predicate.not(PortfolioStatusExcelTableFactoryProportionHelper::isDerivativeCurrencyPairOrCashBalance));
    }

    private static Stream<Table.Record> getRecordStreamForTotalAmount(Table table) {
        return table.stream()
                .filter(PortfolioStatusExcelTableFactoryProportionHelper::hasPositiveCount)
                .filter(Predicate.not(PortfolioStatusExcelTableFactoryProportionHelper::isDerivativeOrCurrencyPair));
    }

    private static boolean hasPositiveCount(Table.Record record) {
        return (record.get(COUNT) instanceof Number n) &&
                n.doubleValue() > 0;
    }

    private static boolean isDerivativeCurrencyPairOrCashBalance(Table.Record record) {
        return isDerivativeOrCurrencyPair(record) ||
                String.valueOf(record.get(SECURITY)).startsWith(CASH_BALANCE);
    }

    private static boolean isDerivativeOrCurrencyPair(Table.Record record) {
        Object type = record.get(TYPE);
        return Objects.equals(type, DERIVATIVE.getDescription()) ||
                Objects.equals(type, CURRENCY_PAIR.getDescription());
    }

    private static BigDecimal getInvestmentAmount(Table.Record record) {
        return toBigDecimal(record.get(AVERAGE_PRICE))
                .add(toBigDecimal(record.get(AVERAGE_ACCRUED_INTEREST)))
                .multiply(toBigDecimal(record.get(COUNT)))
                .subtract(toBigDecimal(record.get(AMORTIZATION)));
    }

    private static BigDecimal getCurrentAmount(Table.Record record) {
        return toBigDecimal(record.get(LAST_PRICE))
                .add(toBigDecimal(record.get(LAST_ACCRUED_INTEREST)))
                .multiply(toBigDecimal(record.get(COUNT)));
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal n) {
            return n;
        }
        return value instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
    }
}

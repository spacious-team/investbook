/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.table_wrapper.api.AnyOfTableColumn;
import org.spacious_team.table_wrapper.api.OptionalTableColumn;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;
import static ru.investbook.parser.uralsib.DerivativeTransactionTable.FortsTableHeader.*;

@Slf4j
public class DerivativeTransactionTable extends SingleAbstractReportTable<DerivativeTransaction> {
    private static final String TABLE_NAME = "СДЕЛКИ С ФЬЮЧЕРСАМИ И ОПЦИОНАМИ";
    private static final Pattern tableEndPredicate = Pattern.compile("^[А-Я\s]+$", UNICODE_CHARACTER_CLASS);
    private boolean expirationTableReached = false;

    public DerivativeTransactionTable(UralsibBrokerReport report) {
        this(report, TABLE_NAME, 2);
    }

    protected DerivativeTransactionTable(UralsibBrokerReport report, String tableName, int headersRowCount) {
        super(report,
                cell -> cell.startsWith(tableName),
                cell -> tableEndPredicate.matcher(cell).matches(),
                FortsTableHeader.class,
                headersRowCount);
    }

    @Override
    protected @Nullable DerivativeTransaction parseRow(TableRow row) {
        if (expirationTableReached) return null;
        String tradeId = SecurityTransactionTable.getTradeId(row, TRANSACTION);
        if (tradeId == null) {
            if (DerivativeExpirationTable.TABLE_NAME.equals(row.getStringCellValueOrDefault(TRANSACTION, null))) {
                expirationTableReached = true;
            }
            return null;
        }

        String direction = row.getStringCellValue(DIRECTION);
        boolean isBuy = direction.equalsIgnoreCase("покупка") || direction.equalsIgnoreCase("зачисление");
        int count = row.getIntCellValue(COUNT);
        BigDecimal valueInPoints = row.getBigDecimalCellValue(QUOTE).multiply(BigDecimal.valueOf(count));
        BigDecimal value = row.getBigDecimalCellValue(VALUE);
        String valueCurrency = UralsibBrokerReport.convertToCurrency(
                row.getStringCellValueOrDefault(VALUE_CURRENCY, "RUB"));
        if (isBuy) {
            value = value.negate();
            valueInPoints = valueInPoints.negate();
        }
        BigDecimal commission = row.getBigDecimalCellValue(MARKET_COMMISSION)
                .add(row.getBigDecimalCellValue(BROKER_COMMISSION))
                .add(row.getBigDecimalCellValueOrDefault(CLEARING_COMMISSION, BigDecimal.ZERO))
                .negate();
        String code = row.getStringCellValue(CONTRACT);
        int securityId = getReport().getSecurityRegistrar().declareDerivative(code);
        return DerivativeTransaction.builder()
                .timestamp(convertToInstant(row.getStringCellValue(DATE_TIME)))
                .tradeId(tradeId)
                .portfolio(getReport().getPortfolio())
                .security(securityId)
                .count((isBuy ? 1 : -1) * count)
                .valueInPoints(valueInPoints)
                .value(value)
                .fee(commission)
                .valueCurrency(valueCurrency)
                .feeCurrency("RUB") // FORTS, only RUB
                .build();
    }

    @RequiredArgsConstructor
    enum FortsTableHeader implements TableHeaderColumn {
        DATE_TIME(AnyOfTableColumn.of(
                PatternTableColumn.of("дата расчетов"),
                PatternTableColumn.of("дата исполнения"))),
        TRANSACTION("номер сделки"),
        TYPE("вид контракта"),
        CONTRACT("наименование контракта"),
        DIRECTION(AnyOfTableColumn.of(
                PatternTableColumn.of("вид сделки"),
                PatternTableColumn.of("зачисление/списание"))),
        COUNT("количество"),
        QUOTE(AnyOfTableColumn.of(
                PatternTableColumn.of("цена фьючерса"),
                PatternTableColumn.of("премия по опциону"))),
        VALUE("^сумма$"),
        VALUE_CURRENCY(OptionalTableColumn.of(
                PatternTableColumn.of("валюта суммы"))), // sometime does not exists
        MARKET_COMMISSION("комиссия тс"),
        BROKER_COMMISSION("комиссия брокера"),
        CLEARING_COMMISSION(OptionalTableColumn.of(
                PatternTableColumn.of("клиринговая комиссия"))); // only for Expiration table

        @Getter
        private final TableColumn column;
        FortsTableHeader(String ... words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}

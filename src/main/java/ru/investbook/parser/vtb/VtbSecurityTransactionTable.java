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

package ru.investbook.parser.vtb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static ru.investbook.parser.vtb.VtbBrokerReport.minValue;
import static ru.investbook.parser.vtb.VtbSecurityTransactionTable.VtbSecurityTransactionTableHeader.*;

public class VtbSecurityTransactionTable extends SingleAbstractReportTable<SecurityTransaction> {

    // Не использовать "Завершенные в отчетном периоде сделки с ценными бумагами (обязательства прекращены)",
    // иначе в расчет не возьмутся позиции, закрытые в течении T+2 (до исполнения)
    private static final String TABLE_NAME = "Заключенные в отчетном периоде сделки с ценными бумагами";
    private final Set<Security> securities = new HashSet<>();

    protected VtbSecurityTransactionTable(SingleBrokerReport report) {
        super(report, TABLE_NAME, null, VtbSecurityTransactionTableHeader.class);
    }

    @Override
    protected SecurityTransaction parseRow(TableRow row) {
        boolean isBuy = row.getStringCellValue(DIRECTION).equalsIgnoreCase("покупка");
        BigDecimal value = row.getBigDecimalCellValue(VALUE_WITH_ACCRUED_INTEREST);
        BigDecimal accruedInterest = row.getBigDecimalCellValue(ACCRUED_INTEREST);
        if (accruedInterest.abs().compareTo(minValue) >= 0) {
            value = value.subtract(accruedInterest);
        } else {
            accruedInterest = BigDecimal.ZERO;
        }
        if (isBuy) {
            value = value.negate();
            accruedInterest = accruedInterest.negate();
        }
        BigDecimal commission = row.getBigDecimalCellValueOrDefault(MARKET_COMMISSION, BigDecimal.ZERO)
                .add(row.getBigDecimalCellValueOrDefault(BROKER_COMMISSION, BigDecimal.ZERO))
                .negate();
        String currency = VtbBrokerReport.convertToCurrency(row.getStringCellValue(VALUE_CURRENCY));

        Security security = VtbReportHelper.getSecurity(row.getStringCellValue(NAME_AND_ISIN));
        securities.add(security);
        int securityId = getReport().getSecurityRegistrar().declareStockOrBondByIsin(security.getIsin(), security::toBuilder);

        return SecurityTransaction.builder()
                .timestamp(row.getInstantCellValue(DATE))
                .tradeId(row.getStringCellValue(TRADE_ID))
                .portfolio(getReport().getPortfolio())
                .security(securityId)
                .count((isBuy ? 1 : -1) * row.getIntCellValue(COUNT))
                .value(value)
                .accruedInterest(accruedInterest)
                .fee(commission)
                .valueCurrency(currency)
                .feeCurrency(currency)
                .build();
    }

    public Set<Security> getSecurities() {
        initializeIfNeed();
        return Set.copyOf(securities);
    }

    @RequiredArgsConstructor
    enum VtbSecurityTransactionTableHeader implements TableHeaderColumn {
        DATE("плановая дата поставки"),
        TRADE_ID("№ сделки"),
        NAME_AND_ISIN("наименование", "isin"),
        DIRECTION("вид сделки"),
        COUNT("количество"),
        VALUE_WITH_ACCRUED_INTEREST("сумма сделки в валюте расчетов", "с учетом НКД"),
        ACCRUED_INTEREST("НКД", "по сделке в валюте расчетов"),
        VALUE_CURRENCY("Валюта расчетов"),
        MARKET_COMMISSION("Комиссия Банка за расчет по сделке"),
        BROKER_COMMISSION("Комиссия Банка за заключение сделки");

        @Getter
        private final TableColumn column;

        VtbSecurityTransactionTableHeader(String ... words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}

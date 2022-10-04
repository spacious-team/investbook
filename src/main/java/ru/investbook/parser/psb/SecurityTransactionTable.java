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

package ru.investbook.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.Security.SecurityBuilder;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.AnyOfTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleInitializableReportTable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.investbook.parser.psb.SecuritiesTable.SecuritiesTableHeader.NAME;
import static ru.investbook.parser.psb.SecurityTransactionTable.TransactionTableHeader.*;

@Slf4j
public class SecurityTransactionTable extends SingleInitializableReportTable<SecurityTransaction> {

    // Использовать в том числе таблицы "... рассчитанные в отчетном периоде",
    // иначе в расчет не возьмутся позиции, закрытые в течении T+2 (до исполнения),
    // в годовых же отчетах сделка уже не встретится в таблице нерассчитанных сделок.
    private static final String[] TABLE_NAMES = {
            "Сделки, совершенные с ЦБ на биржевых торговых площадках (Фондовый рынок) с расчетами в дату заключения",
            "Сделки, совершенные с ЦБ на биржевых торговых площадках (Фондовый рынок) с расчетами Т+, незавершенные в отчетном периоде",
            "Сделки, совершенные с ЦБ на биржевых торговых площадках (Фондовый рынок) с расчетами Т+, рассчитанные в отчетном периоде",
            "Сделки, совершенные с ЦБ на биржевых торговых площадках (Основной рынок ММВБ) с расчетами в дату заключения",
            "Сделки, совершенные с ЦБ на биржевых торговых площадках (Основной рынок ММВБ) с расчетами Т+, незавершенные в отчетном периоде",
            "Сделки, совершенные с ЦБ на биржевых торговых площадках (Основной рынок ММВБ) с расчетами Т+, рассчитанные в отчетном периоде",};
    private static final String TABLE_END_TEXT = "Итого оборот";
    private static final BigDecimal minValue = BigDecimal.valueOf(0.01);
    private final Set<Security> securities = new HashSet<>();

    public SecurityTransactionTable(PsbBrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<SecurityTransaction> parseTable() {
        return Stream.of(TABLE_NAMES)
                .map(this::parseTable)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<SecurityTransaction> parseTable(String tableName) {
        return getReport().getReportPage()
                .create(tableName, TABLE_END_TEXT, TransactionTableHeader.class)
                .excludeTotalRow()
                .getData(getReport(), this::getTransaction);
    }

    private SecurityTransaction getTransaction(TableRow row) {
        boolean isBuy = row.getStringCellValue(DIRECTION).equalsIgnoreCase("покупка");
        BigDecimal value = row.getBigDecimalCellValue(VALUE);
        BigDecimal accruedInterest = row.getBigDecimalCellValue(ACCRUED_INTEREST);
        if (isBuy) {
            value = value.negate();
            accruedInterest = accruedInterest.negate();
        }
        BigDecimal commission = row.getBigDecimalCellValue(MARKET_COMMISSION)
                .add(row.getBigDecimalCellValue(BROKER_COMMISSION))
                .add(row.getBigDecimalCellValue(CLEARING_COMMISSION))
                .add(row.getBigDecimalCellValue(ITS_COMMISSION))
                .negate();
        Security security = getSecurity(row);
        return SecurityTransaction.builder()
                .timestamp(getReport().convertToInstant(row.getStringCellValue(DATE_TIME)))
                .tradeId(String.valueOf(row.getLongCellValue(TRADE_ID))) // may be double numbers in future
                .portfolio(getReport().getPortfolio())
                .security(security.getId())
                .count((isBuy ? 1 : -1) * row.getIntCellValue(COUNT))
                .value(value)
                .accruedInterest((accruedInterest.abs().compareTo(minValue) >= 0) ? accruedInterest : BigDecimal.ZERO)
                .commission(commission)
                .valueCurrency(row.getStringCellValue(VALUE_CURRENCY).replace(" ", "").split("/")[1])
                .commissionCurrency(row.getStringCellValue(COMMISSION_CURRENCY))
                .build();
    }

    private Security getSecurity(TableRow row) {
        String isin = row.getStringCellValue(ISIN);
        SecurityBuilder builder = Security.builder()
                .isin(isin)
                .name(row.getStringCellValue(NAME))
                .type(SecurityType.STOCK_OR_BOND);
        int securityId = getReport().getSecurityRegistrar().declareStockOrBondByIsin(isin, () -> builder);
        Security security = builder.id(securityId).build();
        securities.add(security);
        return security;
    }

    public Set<Security> getSecurities() {
        initializeIfNeed();
        return Set.copyOf(securities);
    }

    enum TransactionTableHeader implements TableColumnDescription {
        DATE_TIME(TableColumnImpl.of("дата", "исполнения"), TableColumnImpl.of("дата и время")),
        TRADE_ID("номер сделки"),
        NAME("наименование"),
        ISIN("isin"),
        DIRECTION("покупка", "продажа"),
        COUNT("кол-во"),
        VALUE("сумма сделки"),
        VALUE_CURRENCY("валюта сделки"),
        ACCRUED_INTEREST("^нкд$"),
        MARKET_COMMISSION("комиссия торговой системы"),
        CLEARING_COMMISSION("клиринговая комиссия"),
        ITS_COMMISSION("комиссия за итс"),
        BROKER_COMMISSION("ком", "брокера"),
        COMMISSION_CURRENCY("валюта", "брок", "комиссии");

        @Getter
        private final TableColumn column;

        TransactionTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }

        TransactionTableHeader(TableColumn... columns) {
            this.column = AnyOfTableColumn.of(columns);
        }
    }
}

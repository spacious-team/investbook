/*
 * Portfolio
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

package ru.portfolio.portfolio.parser.uralsib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;
import ru.portfolio.portfolio.pojo.Security;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static ru.portfolio.portfolio.parser.uralsib.PaymentsTable.PaymentsTableHeader.DESCRIPTION;

@Slf4j
abstract class PaymentsTable<RowType> implements ReportTable<RowType> {

    static final String TABLE_NAME = "ДВИЖЕНИЕ ДЕНЕЖНЫХ СРЕДСТВ ЗА ОТЧЕТНЫЙ ПЕРИОД";
    @Getter
    private final BrokerReport report;
    @Getter
    protected final List<RowType> data = new ArrayList<>();
    // human readable name -> incoming count
    private final List<Map.Entry<Security, Integer>> securitiesIncomingCount;
    private final List<SecurityTransaction> securityTransactions;
    private final ExcelTable table;

    public PaymentsTable(UralsibBrokerReport report,
                         PortfolioSecuritiesTable securitiesTable,
                         SecurityTransactionTable securityTransactionTable) {
        this.report = report;
        this.securitiesIncomingCount = securitiesTable.getData();
        this.securityTransactions = securityTransactionTable.getData();
        this.table = ExcelTable.of(report.getSheet(), TABLE_NAME, PaymentsTableHeader.class);
    }

    protected Collection<RowType> pasreTable() {
        return table.getDataCollection(getReport().getPath(), this::getRow);
    }

    protected abstract Collection<RowType> getRow(ExcelTable table, Row row);

    protected Security getSecurity(ExcelTable table, Row row) {
        String eventDescription = table.getStringCellValue(row, DESCRIPTION);
        String eventDescriptionLowercase = eventDescription.toLowerCase();
        for (Map.Entry<Security, Integer> e : securitiesIncomingCount) {
            Security security = e.getKey();
            String securityName = security.getName();
            if (securityName != null && eventDescriptionLowercase.contains(securityName.toLowerCase())) {
                return security;
            }
        }
        throw new RuntimeException("Не могу найти ISIN ценной бумаги в отчете брокера по событию:" + eventDescription);
    }

    protected Integer getSecurityCount(Security security, Instant atInstant) {
        int count = securitiesIncomingCount.stream()
                .filter(e -> e.getKey().equals(security))
                .map(Map.Entry::getValue)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Не найдено количество на начало периода отчета для ЦБ " + security));
        Collection<SecurityTransaction> transactions = securityTransactions.stream()
                .filter(t -> t.getIsin().equals(security.getIsin()))
                .sorted(Comparator.comparing(SecurityTransaction::getTimestamp))
                .collect(Collectors.toList());
        for (SecurityTransaction transaction : transactions) {
            if (transaction.getTimestamp().isBefore(atInstant)) {
                count += transaction.getCount();
            } else {
                break;
            }
        }
        return count;
    }

    enum PaymentsTableHeader implements TableColumnDescription {
        DATE("дата"),
        OPERATION("тип", "операции"),
        VALUE("сумма"),
        CURRENCY("валюта"),
        DESCRIPTION("комментарий");

        @Getter
        private final TableColumn column;
        PaymentsTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}

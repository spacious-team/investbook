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

package ru.investbook.parser.uralsib;

import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static ru.investbook.parser.uralsib.SecurityRedemptionTable.SecurityFlowTableHeader.*;

/**
 * Ввод и вывод ценных бумаг со счета
 */
public class SecurityDepositAndWithdrawalTable extends AbstractReportTable<SecurityTransaction> {
    private static final String TABLE_NAME = SecurityRedemptionTable.TABLE_NAME;
    private static final String IN_DESCRIPTION = "ввод цб";
    private static final String OUT_DESCRIPTION = "вывод цб";
    private final List<SecuritiesTable.ReportSecurityInformation> securitiesIncomingCount;

    public SecurityDepositAndWithdrawalTable(UralsibBrokerReport report, SecuritiesTable securitiesTable) {
        super(report, TABLE_NAME, "", SecurityRedemptionTable.SecurityFlowTableHeader.class);
        this.securitiesIncomingCount = securitiesTable.getData();
    }

    @Override
    protected Collection<SecurityTransaction> getRow(Table table, TableRow row) {
        String operation = table.getStringCellValue(row, OPERATION);
        if (!operation.equalsIgnoreCase(IN_DESCRIPTION) && !operation.equalsIgnoreCase(OUT_DESCRIPTION)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(SecurityTransaction.builder()
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE)))
                .transactionId(table.getStringCellValue(row, ID))
                .portfolio(getReport().getPortfolio())
                .isin(getSecurity(table, row).getIsin())
                .count(table.getIntCellValue(row, COUNT))
                .value(BigDecimal.ZERO)
                .accruedInterest(BigDecimal.ZERO)
                .commission(BigDecimal.ZERO)
                .valueCurrency("RUB")
                .commissionCurrency("RUB")
                .build());
    }

    private Security getSecurity(Table table, TableRow row) {
        String cfi = table.getStringCellValue(row, CFI);
        return securitiesIncomingCount.stream()
                .filter(security -> security.getCfi().equals(cfi))
                .map(SecuritiesTable.ReportSecurityInformation::getSecurity)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Не могу найти ISIN для ЦБ с CFI = " + cfi));
    }
}

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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;

import static ru.investbook.parser.uralsib.SecurityRedemptionTable.SecurityFlowTableHeader.*;

/**
 * Ввод и вывод ценных бумаг со счета
 */
public class SecurityDepositAndWithdrawalTable extends SingleAbstractReportTable<SecurityTransaction> {
    private static final String TABLE_NAME = SecurityRedemptionTable.TABLE_NAME;
    private static final String IN_DESCRIPTION = "ввод цб";
    private static final String OUT_DESCRIPTION = "вывод цб";
    private final SecuritiesTable securitiesTable;

    public SecurityDepositAndWithdrawalTable(UralsibBrokerReport report, SecuritiesTable securitiesTable) {
        super(report, TABLE_NAME, "", SecurityRedemptionTable.SecurityFlowTableHeader.class);
        this.securitiesTable = securitiesTable;
    }

    @Override
    protected @Nullable SecurityTransaction parseRow(TableRow row) {
        String operation = row.getStringCellValue(OPERATION);
        if (!operation.equalsIgnoreCase(IN_DESCRIPTION) && !operation.equalsIgnoreCase(OUT_DESCRIPTION)) {
            return null;
        }
        return SecurityTransaction.builder()
                .timestamp(convertToInstant(row.getStringCellValue(DATE)))
                .tradeId(row.getStringCellValue(ID))
                .portfolio(getReport().getPortfolio())
                .security(getSecurity(row).getId())
                .count(row.getIntCellValue(COUNT))
                .value(BigDecimal.ZERO)
                .accruedInterest(BigDecimal.ZERO)
                .fee(BigDecimal.ZERO)
                .valueCurrency("RUB")
                .feeCurrency("RUB")
                .build();
    }

    private Security getSecurity(TableRow row) {
        String cfi = row.getStringCellValue(CFI);
        return securitiesTable.getSecurityByCfi(cfi);
    }
}

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

package ru.investbook.parser.investbook;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.parser.SecurityRegistrar;
import ru.investbook.repository.SecurityRepository;

import java.time.Instant;

import static ru.investbook.parser.investbook.AbstractInvestbookTable.InvestbookReportTableHeader.*;

public class InvestbookSecurityDepositAndWithdrawalTable extends AbstractSecurityAwareInvestbookTable<SecurityTransaction> {

    protected InvestbookSecurityDepositAndWithdrawalTable(InvestbookBrokerReport report,
                                                          SecurityRegistrar securityRegistrar,
                                                          SecurityRepository securityRepository,
                                                          SecurityConverter securityConverter) {
        super(report, securityRegistrar, securityRepository, securityConverter);
    }

    @Override
    protected @Nullable SecurityTransaction parseRow(TableRow row) {
        boolean negate;
        String operation = row.getStringCellValue(OPERATION).toLowerCase();
        if (operation.contains("зачисление")) { // Зачисление ЦБ
            negate = false;
        } else if (operation.contains("списание")) { // Списание ЦБ
            negate = true;
        } else {
            return null;
        }
        String portfolio = row.getStringCellValue(PORTFOLIO);
        Instant timestamp = parseEventInstant(row);
        int securityId = getSecurityIdForDepositOrWithdrawal(row);
        return SecurityTransaction.builder()
                .tradeId(getTradeId(portfolio, securityId, timestamp))
                .portfolio(portfolio)
                .timestamp(timestamp)
                .security(securityId)
                .count(row.getIntCellValue(COUNT) * (negate ? -1 : 1))
                .build();
    }

    private int getSecurityIdForDepositOrWithdrawal(TableRow row) {
        String securityTickerNameOrIsin = row.getStringCellValue(TICKER_NAME_ISIN);
        return securityRepository.findByName(securityTickerNameOrIsin)
                .or(() -> securityRepository.findByTicker(securityTickerNameOrIsin))
                .or(() -> securityRepository.findByIsin(securityTickerNameOrIsin))
                .orElseGet(() -> createStockOrBond(securityTickerNameOrIsin))
                .getId();
    }

    private SecurityEntity createStockOrBond(String securityTickerNameOrIsin) {
        Security security = Security.builder()
                .type(SecurityType.STOCK_OR_BOND)
                .name(securityTickerNameOrIsin)
                .build();
        return securityRepository.save(securityConverter.toEntity(security));
    }
}

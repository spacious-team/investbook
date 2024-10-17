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
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.parser.SecurityRegistrar;
import ru.investbook.repository.SecurityRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static ru.investbook.parser.investbook.AbstractInvestbookTable.InvestbookReportTableHeader.*;

public class InvestbookSecurityEventCashFowTable extends AbstractSecurityAwareInvestbookTable<SecurityEventCashFlow> {

    protected InvestbookSecurityEventCashFowTable(BrokerReport report,
                                                  SecurityRegistrar securityRegistrar,
                                                  SecurityRepository securityRepository,
                                                  SecurityConverter securityConverter) {
        super(report, securityRegistrar, securityRepository, securityConverter);
    }

    @Override
    protected Collection<SecurityEventCashFlow> parseRowToCollection(TableRow row) {
        String operation = row.getStringCellValue(OPERATION).toLowerCase();
        CashFlowType type;
        if (operation.contains("дивиденд")) {
            type = CashFlowType.DIVIDEND;
        } else if (operation.contains("купон")) {
                type = CashFlowType.COUPON;
        } else if (operation.contains("амортизац")) { // Амортизация облигации
            type = CashFlowType.AMORTIZATION;
        } else if (operation.contains("погашен")) { // Погашение облигации
            type = CashFlowType.REDEMPTION;
        } else if (operation.contains("вариацион") || operation.contains("маржа")) { // Вариационная маржа
            type = CashFlowType.DERIVATIVE_PROFIT;
        } else {
            return Collections.emptyList();
        }
        String securityTickerNameOrIsin = row.getStringCellValue(TICKER_NAME_ISIN);
        int securityId = switch (type) {
            case DIVIDEND -> getStockOrBondSecurityId(securityTickerNameOrIsin, SecurityType.STOCK);
            case COUPON, AMORTIZATION, REDEMPTION -> getStockOrBondSecurityId(securityTickerNameOrIsin, SecurityType.BOND);
            case DERIVATIVE_PROFIT -> getDerivativeSecurityId(securityTickerNameOrIsin);
            default -> throw new IllegalArgumentException("Unexpected type: " + type);
        };
        Collection<SecurityEventCashFlow> result = new ArrayList<>(2);
        SecurityEventCashFlow.SecurityEventCashFlowBuilder builder = SecurityEventCashFlow.builder()
                .portfolio(row.getStringCellValue(PORTFOLIO))
                .timestamp(row.getInstantCellValue(DATE_TIME))
                .security(securityId)
                .count(row.getIntCellValue(COUNT))
                .eventType(type)
                .value(row.getBigDecimalCellValue(PRICE))
                .currency(row.getStringCellValue(CURRENCY));
        result.add(builder.build());
        @Nullable BigDecimal tax = row.getBigDecimalCellValueOrDefault(FEE, null);
        if (tax != null && Math.abs(tax.floatValue()) > 0.001) {
            result.add(builder
                    .eventType(CashFlowType.TAX)
                    .value(tax.abs().negate())
                    .currency(row.getStringCellValue(FEE_CURRENCY))
                    .build());
        }
        return result;
    }
}

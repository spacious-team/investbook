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

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.AccountCash;
import org.spacious_team.broker.pojo.AccountProperty;
import org.spacious_team.broker.pojo.AccountPropertyType;
import ru.investbook.parser.SingleInitializableReportTable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.report.ForeignExchangeRateService.RUB;

@Slf4j
public class AccountPropertyTable extends SingleInitializableReportTable<AccountProperty> {
    private final AssetsTable securityAssetsTable;
    private final CashTable cashTable;
    private final ForeignExchangeRateTable foreignExchangeRateTable;

    public AccountPropertyTable(AssetsTable securityAssetsTable, CashTable cashTable,
                                ForeignExchangeRateTable foreignExchangeRateTable) {
        super(securityAssetsTable.getReport());
        this.securityAssetsTable = securityAssetsTable;
        this.cashTable = cashTable;
        this.foreignExchangeRateTable = foreignExchangeRateTable;
    }

    @Override
    protected Collection<AccountProperty> parseTable() {
        try {
            Collection<AccountProperty> securityAssets = securityAssetsTable.getData();
            if (!securityAssets.isEmpty()) {
                return securityAssets;
            }

            BigDecimal assets = BigDecimal.ZERO;
            Instant reportEndDateTime = getReport().getReportEndDateTime();
            for (AccountCash cash : cashTable.getData()) {
                BigDecimal value = cash.getValue();
                if (value.floatValue() > 0.001f) {
                    String currency = cash.getCurrency();
                    BigDecimal exchangeRate = foreignExchangeRateTable.getExchangeRate(currency, RUB, reportEndDateTime);
                    assets = assets.add(value.multiply(exchangeRate));
                }
            }

            return singletonList(AccountProperty.builder()
                    .account(getReport().getAccount())
                    .timestamp(reportEndDateTime)
                    .property(AccountPropertyType.TOTAL_ASSETS_RUB)
                    .value(assets.toString())
                    .build());
        } catch (Exception e) {
            log.warn("Не могу получить активы из отчета {}", getReport(), e);
            return emptyList();
        }
    }
}

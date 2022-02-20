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

package ru.investbook.parser.tinkoff;

import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SecurityRegistrar;

import java.math.BigDecimal;
import java.util.Objects;

import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTable.TransactionTableHeader.*;

class TinkoffSecurityTransactionTableHelper {

    static int getSecurityId(TableRow row,
                             SecurityCodeAndIsinTable codeAndIsin,
                             SecurityRegistrar registrar) {
        String code = row.getStringCellValue(CODE);
        String shortName = row.getStringCellValue(SHORT_NAME);
        SecurityType securityType = getSecurityType(row);
        Security security = getSecurity(code, codeAndIsin, shortName, securityType);
        return switch (securityType) {
            case STOCK -> registrar.declareStockByIsin(codeAndIsin.getIsin(code), security::toBuilder);
            case BOND -> registrar.declareBondByIsin(codeAndIsin.getIsin(code), security::toBuilder);
            case STOCK_OR_BOND -> registrar.declareStockOrBondByIsin(codeAndIsin.getIsin(code), security::toBuilder);
            case DERIVATIVE -> registrar.declareDerivative(shortName);
            case CURRENCY_PAIR -> registrar.declareCurrencyPair(shortName);
            case ASSET -> throw new IllegalArgumentException("Тип ASSET не поддерживается");
        };
    }

    static SecurityType getSecurityType(TableRow row) {
        BigDecimal accruedInterest = row.getBigDecimalCellValueOrDefault(ACCRUED_INTEREST, BigDecimal.ZERO);
        if (accruedInterest.floatValue() > 1e-3) {
            return SecurityType.BOND;
        }

        String type = row.getStringCellValueOrDefault(TYPE, "").toUpperCase();
        if (Objects.equals(type, "SPBFUT") ||  Objects.equals(type, "SPBOPT")) { // SPBOPT - догадка
            return SecurityType.DERIVATIVE;
        } else if (Objects.equals(type, "CNGD")) { // это может быть поставочный фьючерс, см. раздел 5 отчета Тинькофф
            return SecurityType.CURRENCY_PAIR;
        }

        String shortName = row.getStringCellValueOrDefault(SHORT_NAME, "");
        if (shortName != null && shortName.length() == 10 && shortName.charAt(6) == '_') { // USDRUB_TOM
            return SecurityType.CURRENCY_PAIR;
        }

        return SecurityType.STOCK;
    }

    private static Security getSecurity(String code,
                                        SecurityCodeAndIsinTable codeAndIsin,
                                        String shortName,
                                        SecurityType securityType) {
        return switch (securityType) {
            case STOCK -> Security.builder()
                    .ticker(code)
                    .isin(codeAndIsin.getIsin(code))
                    .name(shortName)
                    .type(securityType)
                    .build();
            case BOND, STOCK_OR_BOND -> Security.builder()
                    .isin(codeAndIsin.getIsin(code))
                    .name(shortName)
                    .type(securityType)
                    .build();
            case DERIVATIVE, CURRENCY_PAIR -> Security.builder().ticker(shortName).build();
            case ASSET  -> throw new IllegalArgumentException("Произвольный актив не поддерживается");
        };
    }
}

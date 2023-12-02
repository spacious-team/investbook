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

package ru.investbook.parser.tinkoff;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.table_wrapper.api.TableRow;
import org.springframework.stereotype.Component;
import ru.investbook.parser.SecurityRegistrar;
import ru.investbook.service.moex.MoexDerivativeCodeService;

import java.math.BigDecimal;
import java.util.Objects;

import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTable.TransactionTableHeader.*;

@Component
@RequiredArgsConstructor
public class TinkoffSecurityTransactionTableHelper {
    private final MoexDerivativeCodeService moexDerivativeCodeService;

    int getSecurityId(TableRow row,
                      SecurityCodeAndIsinTable codeAndIsin,
                      SecurityRegistrar registrar) {
        String code = row.getStringCellValue(CODE);
        String shortName = row.getStringCellValue(SHORT_NAME);
        SecurityType securityType = getSecurityType(row);
        Security security = getSecurity(code, codeAndIsin, shortName, securityType);
        return declareSecurity(security, registrar);
    }

    SecurityType getSecurityType(TableRow row) {
        BigDecimal accruedInterest = row.getBigDecimalCellValueOrDefault(ACCRUED_INTEREST, BigDecimal.ZERO);
        if (accruedInterest.floatValue() > 1e-3) {
            return SecurityType.BOND;
        }

        String type = row.getStringCellValueOrDefault(TYPE, "").toUpperCase();
        if (Objects.equals(type, "SPBFUT") || Objects.equals(type, "SPBOPT")) { // SPBOPT - догадка
            return SecurityType.DERIVATIVE;
        } else if (Objects.equals(type, "CNGD")) { // это может быть поставочный фьючерс, см. раздел 5 отчета Тинькофф
            return SecurityType.CURRENCY_PAIR;
        }

        String shortName = row.getStringCellValueOrDefault(SHORT_NAME, "");
        String code = row.getStringCellValueOrDefault(CODE, "");
        if (isCurrencyPair(shortName) || isCurrencyPair(code)) { // USDRUB_TOM
            return SecurityType.CURRENCY_PAIR;
        } else if (isDerivative(shortName, code)) {
            return SecurityType.DERIVATIVE;
        }

        return SecurityType.STOCK;
    }

    private static boolean isCurrencyPair(String contract) {
        return contract != null && contract.length() == 10 && contract.charAt(6) == '_';
    }

    private boolean isDerivative(String shortName, String code) {
        return moexDerivativeCodeService.isDerivative(shortName) || moexDerivativeCodeService.isDerivative(code);
    }

    static Security getSecurity(String code,
                                SecurityCodeAndIsinTable codeAndIsin,
                                String shortName,
                                SecurityType securityType) {
        String isin = switch (securityType) {
            case STOCK, BOND, STOCK_OR_BOND -> codeAndIsin.getIsin(code, shortName);
            default -> null;
        };
        return getSecurity(code, isin, shortName, securityType);
    }

    static Security getSecurity(String code,
                                String isin,
                                String shortName,
                                SecurityType securityType) {
        return switch (securityType) {
            case STOCK -> Security.builder()
                    .ticker(code)
                    .isin(isin)
                    .name(shortName)
                    .type(securityType)
                    .build();
            case BOND, STOCK_OR_BOND -> Security.builder()
                    .isin(isin)
                    .name(shortName)
                    .type(securityType)
                    .build();
            case DERIVATIVE -> Security.builder().ticker(shortName).type(SecurityType.DERIVATIVE).build();
            case CURRENCY_PAIR -> Security.builder().ticker(shortName).type(SecurityType.CURRENCY_PAIR).build();
            case ASSET -> throw new IllegalArgumentException("Произвольный актив не поддерживается");
        };
    }

    static int declareSecurity(Security security, SecurityRegistrar registrar) {
        return switch (security.getType()) {
            case STOCK -> registrar.declareStockByIsin(security.getIsin(), security::toBuilder);
            case BOND -> registrar.declareBondByIsin(security.getIsin(), security::toBuilder);
            case STOCK_OR_BOND -> registrar.declareStockOrBondByIsin(security.getIsin(), security::toBuilder);
            case DERIVATIVE -> registrar.declareDerivative(security.getTicker());
            case CURRENCY_PAIR -> registrar.declareCurrencyPair(security.getTicker());
            case ASSET -> throw new IllegalArgumentException("Тип ASSET не поддерживается");
        };
    }
}

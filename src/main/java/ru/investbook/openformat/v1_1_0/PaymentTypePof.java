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

package ru.investbook.openformat.v1_1_0;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.CashFlowType;

@RequiredArgsConstructor
public enum PaymentTypePof {
    CASH("cash"),
    DIVIDEND("dividend"),
    COUPON("coupon"),
    BOND_AMORTIZATION("bond-amortization"),
    BOND_REDEMPTION("bond-redemption"),
    VARIATION_MARGIN("variation-margin"),
    FEE("fee"),
    TAX("tax"),
    INTEREST("interest"),
    OTHER("other");

    @JsonValue
    private final String value;

    static PaymentTypePof valueOf(CashFlowType type) {
        return switch (type) {
            case CASH -> CASH;
            case DIVIDEND -> DIVIDEND;
            case COUPON -> COUPON;
            case AMORTIZATION -> BOND_AMORTIZATION;
            case REDEMPTION -> BOND_REDEMPTION;
            case DERIVATIVE_PROFIT -> VARIATION_MARGIN;
            case COMMISSION -> FEE;
            case TAX -> TAX;
            default -> OTHER;
        };
    }

    CashFlowType toCashFlowType() {
        return switch (this) {
            case CASH -> CashFlowType.CASH;
            case DIVIDEND, INTEREST, OTHER -> CashFlowType.DIVIDEND;
            case COUPON -> CashFlowType.COUPON;
            case BOND_AMORTIZATION -> CashFlowType.AMORTIZATION;
            case BOND_REDEMPTION -> CashFlowType.REDEMPTION;
            case VARIATION_MARGIN -> CashFlowType.DERIVATIVE_PROFIT;
            case FEE -> CashFlowType.COMMISSION;
            case TAX -> CashFlowType.TAX;
        };
    }
}

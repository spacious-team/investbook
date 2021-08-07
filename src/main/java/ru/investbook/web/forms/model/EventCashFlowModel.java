/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.web.forms.model;

import lombok.Data;
import org.spacious_team.broker.pojo.CashFlowType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EventCashFlowModel {

    @Nullable
    private Integer id;

    @NotEmpty
    private String portfolio;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date = LocalDate.now();

    @NotNull
    private CashFlowType type;

    /**
     * Negative value for cash out, otherwise - positive
     */
    @NotNull
    private BigDecimal value;

    @NotEmpty
    private String valueCurrency = "RUB";

    @Nullable
    private String description;

    public void setValueCurrency(String currency) {
        this.valueCurrency = currency.toUpperCase();
    }

    public String getStringType() {
        if (type == null)  return null;
        return switch (type) {
            case DIVIDEND, COUPON, REDEMPTION, AMORTIZATION, ACCRUED_INTEREST -> type.name();
            case CASH -> {
                var desc = String.valueOf(description).toLowerCase();
                if (desc.contains("вычет") && desc.contains("иис")) {
                    yield "TAX_IIS_A";
                }
                yield type.name() + (isValuePositive() ? "_IN" : "_OUT");
            }
            default ->  type.name() + (isValuePositive() ? "_IN" : "_OUT");
        };
    }

    public void setStringType(String value) {
        if (value.equals("TAX_IIS_A")) {
            type = CashFlowType.CASH;
        } else {
            type = CashFlowType.valueOf(value.split("_")[0]);
        }
    }

    private boolean isValuePositive() {
        return value.compareTo(BigDecimal.ZERO) >= 0;
    }
}

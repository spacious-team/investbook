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

package ru.investbook.web.forms.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ForeignExchangeRateModel {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private @NotNull LocalDate date = LocalDate.now();

    private @NotEmpty String baseCurrency;

    @NotEmpty
    String quoteCurrency;

    private @Positive @NotNull BigDecimal rate;

    public void setBaseCurrency(String currency) {
        this.baseCurrency = currency.toUpperCase();
    }

    public void setQuoteCurrency(String currency) {
        this.quoteCurrency = currency.toUpperCase();
    }
}

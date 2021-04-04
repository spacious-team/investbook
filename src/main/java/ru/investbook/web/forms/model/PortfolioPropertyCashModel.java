/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class PortfolioPropertyCashModel extends PortfolioPropertyModel {

    @NotNull
    private BigDecimal cashRub = BigDecimal.ZERO;

    @NotNull
    private BigDecimal cashUsd = BigDecimal.ZERO;

    @NotNull
    private BigDecimal cashEur = BigDecimal.ZERO;

    @NotNull
    private BigDecimal cashGbp = BigDecimal.ZERO;

    @NotNull
    private BigDecimal cashChf = BigDecimal.ZERO;
}

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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.spacious_team.broker.pojo.PortfolioPropertyType;

import java.math.BigDecimal;

import static org.spacious_team.broker.pojo.PortfolioPropertyType.TOTAL_ASSETS_RUB;
import static org.spacious_team.broker.pojo.PortfolioPropertyType.TOTAL_ASSETS_USD;

@Data
@EqualsAndHashCode(callSuper = true)
public class PortfolioPropertyTotalAssetsModel extends PortfolioPropertyModel {

    private BigDecimal totalAssets;

    private Currency totalAssetsCurrency;

    public enum Currency {
        RUB, USD;

        public PortfolioPropertyType toPortfolioProperty() {
            return switch (this) {
                case RUB -> TOTAL_ASSETS_RUB;
                case USD -> TOTAL_ASSETS_USD;
            };
        }

        public static Currency valueOf(PortfolioPropertyType property) {
            return switch (property) {
                case TOTAL_ASSETS_RUB -> RUB;
                case TOTAL_ASSETS_USD -> USD;
            };
        }
    }
}

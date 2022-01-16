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

package ru.investbook.service;

import org.spacious_team.broker.pojo.PortfolioCash;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AssetsAndCashService {

    Set<String> getActivePortfolios();

    Optional<BigDecimal> getTotalAssetsInRub(Collection<String> portfolios);

    Optional<BigDecimal> getTotalAssetsInRub(String portfolio);

    Optional<BigDecimal> getTotalCashInRub(Collection<String> portfolios);

    /**
     * Возвращает для портфеля последний известный остаток денежных средств соответствующей дате, не позже указанной.
     * Если портфель не указан, возвращает для всех портфелей последние известные остатки денежных средств
     * соответствующих дате, не позже указанной. Записи в результирующем списке отсортированы по времени от новых к старым.
     */
    List<PortfolioCash> getPortfolioCash(Collection<String> portfolios, Instant atInstant);
}

/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.PortfolioCashEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Transactional(readOnly = true)
public interface PortfolioCashRepository extends JpaRepository<PortfolioCashEntity, Integer> {

    List<PortfolioCashEntity> findByPortfolioIn(Collection<String> portfolios);

    List<PortfolioCashEntity> findByPortfolioInOrderByTimestampDesc(Collection<String> portfolios);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM portfolio_cash AS t1
            WHERE timestamp = (
                SELECT MAX(timestamp)
                FROM portfolio_cash AS t2
                WHERE t1.portfolio = t2.portfolio
                AND t2.timestamp between :from AND :to
            )
            ORDER BY portfolio, timestamp DESC
            """)
    List<PortfolioCashEntity> findDistinctOnPortfolioByTimestampBetweenOrderByTimestampDesc(
            @Param("from") Instant startDate,
            @Param("to") Instant endDate);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM portfolio_cash AS t1
            WHERE portfolio IN (:portfolios)
            AND timestamp = (
                SELECT MAX(timestamp)
                FROM portfolio_cash AS t2
                WHERE t1.portfolio = t2.portfolio
                AND t2.timestamp between :from AND :to
            )
            ORDER BY portfolio, timestamp DESC
            """)
    List<PortfolioCashEntity> findDistinctOnPortfolioByPortfolioInAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolios") Collection<String> portfolios,
            @Param("from") Instant startDate,
            @Param("to") Instant endDate);

}

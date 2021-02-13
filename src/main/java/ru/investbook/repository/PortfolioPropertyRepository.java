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

package ru.investbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.investbook.entity.PortfolioPropertyEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PortfolioPropertyRepository extends JpaRepository<PortfolioPropertyEntity, Integer> {

    List<PortfolioPropertyEntity> findByPropertyInOrderByTimestampDesc(Collection<String> property);

    Optional<PortfolioPropertyEntity> findFirstByPropertyOrderByTimestampDesc(String property);

    Optional<PortfolioPropertyEntity> findFirstByPortfolioIdAndPropertyOrderByTimestampDesc(String portfolio,
                                                                                            String property);

    Optional<PortfolioPropertyEntity> findFirstByPortfolioIdAndPropertyAndTimestampBetweenOrderByTimestampDesc(String portfolio,
                                                                                                               String property,
                                                                                                               Instant startDate,
                                                                                                               Instant endDate);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM portfolio_property AS t1
            WHERE property = :property 
            AND timestamp = (
                SELECT MAX(timestamp)
                FROM portfolio_property AS t2
                WHERE t1.portfolio = t2.portfolio
                AND t2.property = :property 
                AND t2.timestamp between :from AND :to
            )
            ORDER BY portfolio, timestamp DESC
            """)
    List<PortfolioPropertyEntity> findDistinctOnPortfolioByPropertyAndTimestampBetweenOrderByTimestampDesc(
            @Param("property") String property,
            @Param("from") Instant startDate,
            @Param("to") Instant endDate);

    List<PortfolioPropertyEntity> findByPropertyAndTimestampBetweenOrderByTimestampDesc(String property,
                                                                                        Instant startDate,
                                                                                        Instant endDate);

    List<PortfolioPropertyEntity> findByPortfolioIdAndPropertyAndTimestampBetweenOrderByTimestampDesc(String portfolio,
                                                                                                    String property,
                                                                                                    Instant startDate,
                                                                                                    Instant endDate);
}

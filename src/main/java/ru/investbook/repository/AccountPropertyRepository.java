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

package ru.investbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.ListQuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.AccountEntity;
import ru.investbook.entity.AccountPropertyEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public interface AccountPropertyRepository extends
        JpaRepository<AccountPropertyEntity, Integer>,
        JpaSpecificationExecutor<AccountPropertyEntity>,
        ListQuerydslPredicateExecutor<AccountPropertyEntity> {

    Optional<AccountPropertyEntity> findFirstByOrderByTimestampDesc();

    List<AccountPropertyEntity> findByAccountInAndPropertyInOrderByTimestampDesc(
            Collection<AccountEntity> accounts,
            Collection<String> property);

    Optional<AccountPropertyEntity> findFirstByAccountIdAndPropertyOrderByTimestampDesc(String account,
                                                                                        String property);

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
    List<AccountPropertyEntity> findDistinctOnAccountIdByPropertyAndTimestampBetweenOrderByTimestampDesc(
            @Param("property") String property,
            @Param("from") Instant startDate,
            @Param("to") Instant endDate);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM portfolio_property AS t1
            WHERE portfolio IN (:accounts)
            AND property = :property
            AND timestamp = (
                SELECT MAX(timestamp)
                FROM portfolio_property AS t2
                WHERE t1.portfolio = t2.portfolio
                AND t2.property = :property
                AND t2.timestamp between :from AND :to
            )
            ORDER BY portfolio, timestamp DESC
            """)
    List<AccountPropertyEntity> findDistinctOnAccountIdByAccountIdInAndPropertyAndTimestampBetweenOrderByTimestampDesc(
            @Param("accounts") Collection<String> accounts,
            @Param("property") String property,
            @Param("from") Instant startDate,
            @Param("to") Instant endDate);

    List<AccountPropertyEntity> findByPropertyInAndTimestampBetweenOrderByTimestampAsc(Collection<String> properties,
                                                                                       Instant startDate,
                                                                                       Instant endDate);

    List<AccountPropertyEntity> findByAccountIdInAndPropertyInAndTimestampBetweenOrderByTimestampAsc(
            Collection<String> accounts,
            Collection<String> properties,
            Instant startDate,
            Instant endDate);
}

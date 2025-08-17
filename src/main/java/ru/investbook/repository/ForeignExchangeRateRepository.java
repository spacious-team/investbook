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
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.ForeignExchangeRateEntity;
import ru.investbook.entity.ForeignExchangeRateEntityPk;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public interface ForeignExchangeRateRepository extends
        JpaRepository<ForeignExchangeRateEntity, ForeignExchangeRateEntityPk>,
        JpaSpecificationExecutor<ForeignExchangeRateEntity>,
        QuerydslPredicateExecutor<ForeignExchangeRateEntity>
{

    Optional<ForeignExchangeRateEntity> findByPkCurrencyPairAndPkDate(String currencyPair, LocalDate atDate);

    Optional<ForeignExchangeRateEntity> findFirstByPkCurrencyPairOrderByPkDateDesc(String currencyPair);

    List<ForeignExchangeRateEntity> findByPkCurrencyPairOrderByPkDateDesc(String currencyPair);

    @Query(value = """
        SELECT max(t.pk.date)
            FROM ForeignExchangeRateEntity t
            GROUP BY t.pk.currencyPair
    """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    Collection<LocalDate> findByMaxPkDateGroupByPkCurrencyPair();

}

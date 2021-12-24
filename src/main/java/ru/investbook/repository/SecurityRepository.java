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

import org.spacious_team.broker.pojo.SecurityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.SecurityEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
public interface SecurityRepository extends JpaRepository<SecurityEntity, Integer> {

    Optional<SecurityEntity> findByIsin(String isin);

    Optional<SecurityEntity> findByTicker(String ticker);

    Optional<SecurityEntity> findByName(String name);

    Collection<SecurityEntity> findByType(SecurityType securityType);

    Collection<SecurityEntity> findByTypeIn(Collection<SecurityType> securityType);

    /**
     * @return in USDRUB format
     */
    default Optional<String> findCurrencyPair(Integer securityId) {
        return findById(securityId)
                .filter(security -> security.getType() == SecurityType.CURRENCY_PAIR)
                .map(SecurityEntity::getTicker)
                .map(SecurityType::getCurrencyPair);
    }

    /**
     * @param securityIds can hold different contracts (USDRUB_TOD, USDRUB_TOM, etc.) of same currency pair (USDRUB)
     * @return only (any) security id for currency pair
     */
    default Collection<Integer> findDistinctContractForCurrencyPair(Collection<Integer> securityIds) {
        return findDistinctCurrencyPair(securityIds)
                .stream()
                .map(this::findFirstByCurrencyPair)
                .flatMap(Optional::stream)
                .map(SecurityEntity::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Returns foreign exchange market currency pairs (in USDRUB format)
     */
    default List<String> findDistinctCurrencyPair(Collection<Integer> securityIds) {
        return securityIds.stream()
                .map(this::findCurrencyPair)
                .flatMap(Optional::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * @param currencyPair in USDRUB format
     */
    @Query(nativeQuery = true, value = """
            SELECT * from security
            WHERE ticker LIKE CONCAT(:currencyPair, '\\_%')
            LIMIT 1
            """)
    Optional<SecurityEntity> findFirstByCurrencyPair(String currencyPair);
}

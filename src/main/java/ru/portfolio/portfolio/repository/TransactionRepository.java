/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.portfolio.portfolio.entity.TransactionEntity;
import ru.portfolio.portfolio.entity.TransactionEntityPK;
import ru.portfolio.portfolio.pojo.Portfolio;

import java.util.ArrayList;
import java.util.Collection;

public interface TransactionRepository extends JpaRepository<TransactionEntity, TransactionEntityPK> {

    /**
     * Returns stock market share and bonds ISINs
     */
    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND length(isin) = 12 " +
            "ORDER BY timestamp DESC")
    Collection<String> findDistinctIsinByPortfolioOrderByTimestampDesc(@Param("portfolio") Portfolio portfolio);

    /**
     * Returns derivatives market contracts
     */
    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND length(isin) <> 12 " +
            "ORDER BY timestamp DESC")
    Collection<String> findDistinctDerivativeByPortfolioOrderByTimestampDesc(@Param("portfolio") Portfolio portfolio);

    ArrayList<TransactionEntity> findBySecurityIsinAndPkPortfolioOrderByTimestampAscPkIdAsc(String isin,
                                                                                            String portfolio);
}

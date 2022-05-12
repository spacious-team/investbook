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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.PortfolioEntity;

import java.util.Set;

@Transactional(readOnly = true)
public interface PortfolioRepository extends JpaRepository<PortfolioEntity, String> {

    Set<PortfolioEntity> findByEnabledIsTrue();

    Set<PortfolioEntity> findByEnabledIsFalse();

    @Transactional
    @Modifying
    @Query("UPDATE PortfolioEntity SET enabled = :enabled WHERE id = :portfolio")
    void setEnabledForPortfolio(String portfolio, boolean enabled);
}

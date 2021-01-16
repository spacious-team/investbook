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
import ru.investbook.entity.SecurityEventCashFlowEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SecurityEventCashFlowRepository extends JpaRepository<SecurityEventCashFlowEntity, Integer> {

    List<SecurityEventCashFlowEntity> findByPortfolioIdAndSecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
            String portfolio,
            String isin,
            int cashFlowType,
            Instant fromDate,
            Instant toDate);

    List<SecurityEventCashFlowEntity> findBySecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
            String isin,
            int cashFlowType,
            Instant fromDate,
            Instant toDate);

    List<SecurityEventCashFlowEntity> findByPortfolioIdAndSecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampAsc(
            String portfolio,
            String isin,
            Set<Integer> cashFlowType,
            Instant fromDate,
            Instant toDate);

    List<SecurityEventCashFlowEntity> findBySecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampAsc(
            String isin,
            Set<Integer> cashFlowType,
            Instant fromDate,
            Instant toDate);

    /**
     * Return all portfolio payments, between date-time interval
     */
    List<SecurityEventCashFlowEntity> findByPortfolioIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
            String portfolio,
            Set<Integer> cashFlowType,
            Instant fromDate,
            Instant toDate);

    /**
     * Return last security payment, between date-time interval
     */
    Optional<SecurityEventCashFlowEntity> findFirstByPortfolioIdAndSecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
            String portfolio,
            String isin,
            Set<Integer> cashFlowType,
            Instant fromDate,
            Instant toDate);

    /**
     * Return last security payment, between date-time interval
     */
    Optional<SecurityEventCashFlowEntity> findFirstBySecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
            String isin,
            Set<Integer> cashFlowType,
            Instant fromDate,
            Instant toDate);
}
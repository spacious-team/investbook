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
import org.springframework.data.querydsl.ListQuerydslPredicateExecutor;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.SecurityEventCashFlowEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Transactional(readOnly = true)
public interface SecurityEventCashFlowRepository extends
        JpaRepository<SecurityEventCashFlowEntity, Integer>,
        JpaSpecificationExecutor<SecurityEventCashFlowEntity>,
        ListQuerydslPredicateExecutor<SecurityEventCashFlowEntity> {

    Optional<SecurityEventCashFlowEntity> findFirstByOrderByTimestampDesc();

    List<SecurityEventCashFlowEntity> findByCashFlowTypeId(int type);

    Optional<SecurityEventCashFlowEntity> findByAccountIdAndSecurityIdAndCashFlowTypeIdAndTimestampAndCount(
            String account,
            Integer securityId,
            int cashFlowType,
            Instant timestamp,
            int count);

    List<SecurityEventCashFlowEntity> findByAccountIdInAndSecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
            Collection<String> accounts,
            Integer securityId,
            int cashFlowType,
            Instant fromDate,
            Instant toDate);

    List<SecurityEventCashFlowEntity> findBySecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
            Integer securityId,
            int cashFlowType,
            Instant fromDate,
            Instant toDate);

    List<SecurityEventCashFlowEntity> findByAccountIdInAndSecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampAsc(
            Collection<String> accounts,
            Integer securityId,
            Set<Integer> cashFlowType,
            Instant fromDate,
            Instant toDate);

    List<SecurityEventCashFlowEntity> findBySecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampAsc(
            Integer securityId,
            Set<Integer> cashFlowType,
            Instant fromDate,
            Instant toDate);

    /**
     * Return all account payments, between date-time interval
     */
    List<SecurityEventCashFlowEntity> findByAccountIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
            String account,
            Set<Integer> cashFlowType,
            Instant fromDate,
            Instant toDate);

    /**
     * Return last security payment, between date-time interval
     */
    Optional<SecurityEventCashFlowEntity> findFirstByAccountIdInAndSecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
            Collection<String> accounts,
            Integer securityId,
            Set<Integer> cashFlowType,
            Instant fromDate,
            Instant toDate);

    /**
     * Return last security payment, between date-time interval
     */
    Optional<SecurityEventCashFlowEntity> findFirstBySecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
            Integer securityId,
            Set<Integer> cashFlowType,
            Instant fromDate,
            Instant toDate);
}
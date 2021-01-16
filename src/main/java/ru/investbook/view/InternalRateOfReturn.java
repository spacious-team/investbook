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

package ru.investbook.view;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.decampo.xirr.NewtonRaphson;
import org.decampo.xirr.Xirr;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.stereotype.Component;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.TransactionCashFlowRepository;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.spacious_team.broker.pojo.CashFlowType.*;
import static org.spacious_team.broker.pojo.SecurityType.DERIVATIVE;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalRateOfReturn {
    private final PositionsFactory positionsFactory;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final ZoneId zoneId = ZoneId.systemDefault();
    private final Xirr.Builder xirrBuilder = Xirr.builder()
            .withNewtonRaphsonBuilder(NewtonRaphson.builder().withTolerance(0.001)); // in currency units (RUB, USD)
    private final Set<Integer> paymentTypes = Set.of(
            DIVIDEND.getId(),
            COUPON.getId(),
            AMORTIZATION.getId(),
            REDEMPTION.getId(),
            TAX.getId());

    /**
     * Возвращает внутреннюю норму доходности вложений. Не рассчитывается для срочных инструментов, т.к.
     * вложение (гарантийное обеспечение) не хранится на данный момент в БД.
     * @param currentQuote may be null only if current security position is zero
     * @return internal rate of return if can be calculated or null otherwise
     */
    public Double calc(Optional<Portfolio> portfolio, Security security, SecurityQuote currentQuote, ViewFilter filter) {
        try {
            if (SecurityType.getSecurityType(security.getId()) == DERIVATIVE) {
                return null;
            }
            Positions positions = positionsFactory.get(portfolio, security, filter);
            int count = positions.getCurrentOpenedPositionsCount();
            if (count != 0 && (currentQuote == null || currentQuote.getDirtyPriceInCurrency() == null)) {
                return null;
            }

            Collection<org.decampo.xirr.Transaction> transactions = positions.getTransactions()
                    .stream()
                    .map(this::castToXirrTransaction)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            getSecurityEventCashFlowEntities(portfolio, security, paymentTypes)
                    .stream()
                    .map(this::castToXirrTransaction)
                    .collect(Collectors.toCollection(() -> transactions));

            castToXirrTransaction(currentQuote, count)
                    .ifPresent(transactions::add);

            return xirrBuilder
                    .withTransactions(transactions)
                    .xirr();
        } catch (Exception e) {
            log.debug("Ошибка вычисления внутренней нормы доходности для бумаги {}", security, e);
            return null;
        }
    }

    private Optional<org.decampo.xirr.Transaction> castToXirrTransaction(Transaction transaction) {
        return getTransactionValue(transaction)
                .map(value -> new org.decampo.xirr.Transaction(
                        value.doubleValue(),
                        transaction.getTimestamp()
                                .atZone(zoneId)
                                .toLocalDate()));
    }

    private org.decampo.xirr.Transaction castToXirrTransaction(SecurityEventCashFlowEntity cashFlowEntity) {
        return new org.decampo.xirr.Transaction(
                cashFlowEntity.getValue().doubleValue(),
                cashFlowEntity.getTimestamp()
                        .atZone(zoneId)
                        .toLocalDate());
    }

    private Optional<org.decampo.xirr.Transaction> castToXirrTransaction(SecurityQuote quote, int positionCount) {
        return Optional.ofNullable(quote)
                .map(SecurityQuote::getDirtyPriceInCurrency)
                .map(dirtyPrice -> new org.decampo.xirr.Transaction(
                        positionCount * dirtyPrice.doubleValue(),
                        quote.getTimestamp()
                                .atZone(zoneId)
                                .toLocalDate()));
    }

    private Optional<BigDecimal> getTransactionValue(Transaction t) {
        if (t.getId() == null) { // bond redemption, accounted by other way, skipping
            return Optional.empty();
        }
        return transactionCashFlowRepository
                .findByPkPortfolioAndPkTransactionIdAndPkType(t.getPortfolio(), t.getId(), PRICE.getId())
                .map(price ->
                    transactionCashFlowRepository
                            .findByPkPortfolioAndPkTransactionIdAndPkType(t.getPortfolio(), t.getId(), COMMISSION.getId())
                            .filter(comission -> comission.getCurrency().equals(price.getCurrency()))
                            .map(commission -> price.getValue().add(commission.getValue()))
                            .orElse(price.getValue()));
    }

    public List<SecurityEventCashFlowEntity> getSecurityEventCashFlowEntities(Optional<Portfolio> portfolio,
                                                                              Security security,
                                                                              Set<Integer> cashFlowTypes) {
        return portfolio
                .map(value ->
                        securityEventCashFlowRepository
                                .findByPortfolioIdAndSecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampAsc(
                                        value.getId(),
                                        security.getId(),
                                        cashFlowTypes,
                                        ViewFilter.get().getFromDate(),
                                        ViewFilter.get().getToDate()))
                .orElseGet(() ->
                        securityEventCashFlowRepository
                                .findBySecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampAsc(
                                        security.getId(),
                                        cashFlowTypes,
                                        ViewFilter.get().getFromDate(),
                                        ViewFilter.get().getToDate()));
    }
}

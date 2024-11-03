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

package ru.investbook.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.decampo.xirr.NewtonRaphson;
import org.decampo.xirr.Xirr;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.stereotype.Component;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.TransactionCashFlowRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static org.spacious_team.broker.pojo.CashFlowType.*;
import static org.spacious_team.broker.pojo.SecurityType.DERIVATIVE;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalRateOfReturn {
    private final FifoPositionsFactory positionsFactory;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final ForeignExchangeRateService foreignExchangeRateService;
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
     *
     * @param quote may be null, if current security position equals to 0. Otherwise, null result is returned
     * @return internal rate of return, if it can be calculated, or null otherwise
     */
    public @Nullable Double calc(
            Collection<String> portfolios, Security security, @Nullable SecurityQuote quote, Instant fromDate, Instant toDate) {

        try {
            boolean isDerivative = (security.getType() == DERIVATIVE);
            if (isDerivative) {
                return null;
            }
            FifoPositionsFilter pf = FifoPositionsFilter.of(portfolios, fromDate, toDate);
            FifoPositions positions = positionsFactory.get(security, pf);
            int count = positions.getCurrentOpenedPositionsCount();
            if (count != 0 && (quote == null || quote.getDirtyPriceInCurrency(isDerivative) == null)) {
                return null;
            }

            String toCurrency = getTransactionCurrency(positions);
            Collection<org.decampo.xirr.Transaction> transactions = positions.getTransactions()
                    .stream()
                    .map(transaction -> castToXirrTransaction(transaction, toCurrency))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            getSecurityEventCashFlowEntities(portfolios, security, paymentTypes)
                    .stream()
                    .map(cash -> castToXirrTransaction(cash, toCurrency))
                    .collect(toCollection(() -> transactions));

            castToXirrTransaction(quote, toCurrency, count, security.getType())
                    .ifPresent(transactions::add);

            return xirrBuilder
                    .withTransactions(transactions)
                    .xirr();
        } catch (Exception e) {
            log.debug("Ошибка вычисления внутренней нормы доходности для бумаги {}", security, e);
            return null;
        }
    }

    private String getTransactionCurrency(FifoPositions positions) {
        return positions.getTransactions()
                .stream()
                .map(t -> transactionCashFlowRepository.findByTransactionIdAndCashFlowType(requireNonNull(t.getId()), PRICE))
                .flatMap(Optional::stream)
                .map(TransactionCashFlowEntity::getCurrency)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Can't find any transaction payment currency"));
    }

    private Optional<org.decampo.xirr.Transaction> castToXirrTransaction(Transaction transaction, String toCurrency) {
        return getTransactionValue(transaction, toCurrency)
                .map(value -> new org.decampo.xirr.Transaction(
                        value.doubleValue(),
                        toLocalDate(transaction.getTimestamp())));
    }

    private org.decampo.xirr.Transaction castToXirrTransaction(SecurityEventCashFlowEntity cashFlowEntity, String toCurrency) {
        BigDecimal value = convertToCurrency(cashFlowEntity.getValue(), cashFlowEntity.getCurrency(), toCurrency);
        return new org.decampo.xirr.Transaction(
                value.doubleValue(),
                toLocalDate(cashFlowEntity.getTimestamp()));
    }

    private Optional<org.decampo.xirr.Transaction> castToXirrTransaction(@Nullable SecurityQuote quote,
                                                                         String toCurrency, int positionCount,
                                                                         SecurityType securityType) {
        return ofNullable(quote)
                .map(_quote -> _quote.getDirtyPriceInCurrency(securityType == DERIVATIVE))
                .map(dirtyPrice -> convertToCurrency(dirtyPrice, requireNonNull(quote.getCurrency()), toCurrency))
                .map(dirtyPrice -> new org.decampo.xirr.Transaction(
                        positionCount * dirtyPrice.doubleValue(),
                        toLocalDate(quote.getTimestamp())));
    }

    private Optional<BigDecimal> getTransactionValue(Transaction t, String toCurrency) {
        @Nullable BigDecimal value = null;
        @Nullable Integer transactionId = t.getId();
        if (transactionId != null) { // bond redemption, accounted by other way, skipping
            value = transactionCashFlowRepository.findByTransactionId(transactionId)
                    .stream()
                    .map(entity -> convertToCurrency(entity.getValue(), entity.getCurrency(), toCurrency))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return (BigDecimal.ZERO.equals(value)) ? empty() : ofNullable(value);
    }

    public List<SecurityEventCashFlowEntity> getSecurityEventCashFlowEntities(Collection<String> portfolios,
                                                                              Security security,
                                                                              Set<Integer> cashFlowTypes) {
        return portfolios.isEmpty() ?
                securityEventCashFlowRepository
                        .findBySecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampAsc(
                                requireNonNull(security.getId()),
                                cashFlowTypes,
                                ViewFilter.get().getFromDate(),
                                ViewFilter.get().getToDate()) :
                securityEventCashFlowRepository
                        .findByPortfolioIdInAndSecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampAsc(
                                portfolios,
                                requireNonNull(security.getId()),
                                cashFlowTypes,
                                ViewFilter.get().getFromDate(),
                                ViewFilter.get().getToDate());
    }

    private BigDecimal convertToCurrency(BigDecimal value, String fromCurrency, String toCurrency) {
        return foreignExchangeRateService.convertValueToCurrency(value, fromCurrency, toCurrency);
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant.atZone(zoneId).toLocalDate();
    }
}

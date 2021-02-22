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
import ru.investbook.entity.TransactionCashFlowEntity;
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
     * @param currentQuote may be null only if current security position is zero
     * @param quoteCurrency quote currency
     * @return internal rate of return if can be calculated or null otherwise
     */
    // TODO convert all values to same currency
    public Double calc(Optional<Portfolio> portfolio, Security security,
                       SecurityQuote currentQuote, String quoteCurrency, ViewFilter filter) {
        try {
            if (SecurityType.getSecurityType(security.getId()) == DERIVATIVE) {
                return null;
            }
            FifoPositions positions = positionsFactory.get(portfolio, security, filter);
            int count = positions.getCurrentOpenedPositionsCount();
            if (count != 0 && (currentQuote == null || currentQuote.getDirtyPriceInCurrency() == null)) {
                return null;
            }

            String toCurrency = getTransactionCurrency(positions);
            Collection<org.decampo.xirr.Transaction> transactions = positions.getTransactions()
                    .stream()
                    .map(transaction -> castToXirrTransaction(transaction, toCurrency))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            getSecurityEventCashFlowEntities(portfolio, security, paymentTypes)
                    .stream()
                    .map(cash -> castToXirrTransaction(cash, toCurrency))
                    .collect(Collectors.toCollection(() -> transactions));

            castToXirrTransaction(currentQuote, quoteCurrency, toCurrency, count)
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
                .map(t -> transactionCashFlowRepository
                        .findByPkPortfolioAndPkTransactionIdAndPkType(t.getPortfolio(), t.getId(), PRICE.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(TransactionCashFlowEntity::getCurrency)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Can't find any transaction payment currency"));
    }

    private Optional<org.decampo.xirr.Transaction> castToXirrTransaction(Transaction transaction, String toCurrency) {
        return getTransactionValue(transaction, toCurrency)
                .map(value -> new org.decampo.xirr.Transaction(
                        value.doubleValue(),
                        transaction.getTimestamp()
                                .atZone(zoneId)
                                .toLocalDate()));
    }

    private org.decampo.xirr.Transaction castToXirrTransaction(SecurityEventCashFlowEntity cashFlowEntity, String toCurrency) {
        BigDecimal value = convertToCurrency(cashFlowEntity.getValue(), cashFlowEntity.getCurrency(), toCurrency);
        return new org.decampo.xirr.Transaction(
                value.doubleValue(),
                cashFlowEntity.getTimestamp()
                        .atZone(zoneId)
                        .toLocalDate());
    }

    private Optional<org.decampo.xirr.Transaction> castToXirrTransaction(SecurityQuote quote, String quoteCurrency,
                                                                         String toCurrency, int positionCount) {
        return Optional.ofNullable(quote)
                .map(SecurityQuote::getDirtyPriceInCurrency)
                .map(dirtyPrice -> convertToCurrency(dirtyPrice, quoteCurrency, toCurrency))
                .map(dirtyPrice -> new org.decampo.xirr.Transaction(
                        positionCount * dirtyPrice.doubleValue(),
                        quote.getTimestamp()
                                .atZone(zoneId)
                                .toLocalDate()));
    }

    private Optional<BigDecimal> getTransactionValue(Transaction t, String toCurrency) {
        BigDecimal value = null;
        if (t.getId() != null) { // bond redemption, accounted by other way, skipping
            value = transactionCashFlowRepository.findByPkPortfolioAndPkTransactionId(t.getPortfolio(), t.getId())
                    .stream()
                    .map(entity -> convertToCurrency(entity.getValue(), entity.getCurrency(), toCurrency))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return (BigDecimal.ZERO.equals(value)) ? Optional.empty() : Optional.ofNullable(value);
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

    private BigDecimal convertToCurrency(BigDecimal value, String fromCurrency, String toCurrency) {
        return foreignExchangeRateService.convertValueToCurrency(value, fromCurrency, toCurrency);
    }
}

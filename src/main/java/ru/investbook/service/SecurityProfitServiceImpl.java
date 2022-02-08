/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.service;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.stereotype.Service;
import ru.investbook.converter.SecurityQuoteConverter;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.report.ClosedPosition;
import ru.investbook.report.FifoPositions;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.report.OpenedPosition;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.SecurityQuoteRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.spacious_team.broker.pojo.SecurityType.CURRENCY_PAIR;
import static org.springframework.util.StringUtils.hasLength;

@Service
@RequiredArgsConstructor
public class SecurityProfitServiceImpl implements SecurityProfitService {

    private final Set<Integer> priceAndAccruedInterestTypes = Set.of(CashFlowType.PRICE.getId(), CashFlowType.ACCRUED_INTEREST.getId());
    private final SecurityRepository securityRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final SecurityQuoteRepository securityQuoteRepository;
    private final SecurityQuoteConverter securityQuoteConverter;
    private final ForeignExchangeRateService foreignExchangeRateService;

    @Override
    public Optional<Instant> getLastEventTimestamp(
            Collection<String> portfolios, Security security, Set<Integer> events, Instant from, Instant to) {

        Optional<SecurityEventCashFlowEntity> optional = portfolios.isEmpty() ?
                securityEventCashFlowRepository
                        .findFirstBySecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
                                security.getId(), events, from, to) :
                securityEventCashFlowRepository
                        .findFirstByPortfolioIdInAndSecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
                                portfolios, security.getId(), events, from, to);
        return optional.map(SecurityEventCashFlowEntity::getTimestamp);
    }

    @Override
    public BigDecimal getGrossProfit(Collection<String> portfolios, Security security, FifoPositions positions, String toCurrency) {
        return switch (security.getType()) {
            case STOCK, BOND, STOCK_OR_BOND, ASSET -> getPurchaseCost(security, positions, toCurrency)
                    .add(getPurchaseAccruedInterest(security, positions, toCurrency));
            case DERIVATIVE -> sumPaymentsForType(portfolios, security, CashFlowType.DERIVATIVE_PROFIT, toCurrency);
            case CURRENCY_PAIR -> getPurchaseCost(security, positions, toCurrency);
        };
    }

    @Override
    public BigDecimal getPurchaseCost(Security security, FifoPositions positions, String toCurrency) {
        return switch (security.getType()) {
            case STOCK, BOND, STOCK_OR_BOND, ASSET -> getStockOrBondPurchaseCost(positions, toCurrency);
            case DERIVATIVE -> getTotal(positions.getTransactions(), CashFlowType.DERIVATIVE_PRICE, toCurrency);
            case CURRENCY_PAIR -> getTotal(positions.getTransactions(), CashFlowType.PRICE, toCurrency);
        };
    }

    /**
     * Разница цен продаж и покупок. Не учитывается цена покупки, если ЦБ выведена со счета, не учитывается цена
     * продажи, если ЦБ введена на счет
     */
    private BigDecimal getStockOrBondPurchaseCost(FifoPositions positions, String toCurrency) {
        BigDecimal purchaseCost = positions.getOpenedPositions()
                .stream()
                .map(openPosition -> getTransactionValue(openPosition.getOpenTransaction(), CashFlowType.PRICE, toCurrency)
                        .map(value -> value.multiply(getOpenAmountMultiplier(openPosition))))
                .flatMap(Optional::stream)
                .reduce(BigDecimal.ZERO, BigDecimal::add); // если ценная бумага не вводилась на счет, а была куплена (есть цена покупки)
        for (ClosedPosition closedPosition : positions.getClosedPositions()) {
            BigDecimal openPrice = getTransactionValue(closedPosition.getOpenTransaction(), CashFlowType.PRICE, toCurrency)
                    .map(value -> value.multiply(getOpenAmountMultiplier(closedPosition)))
                    .orElse(null);
            BigDecimal closePrice = getTransactionValue(closedPosition.getCloseTransaction(), CashFlowType.PRICE, toCurrency)
                    .map(value -> value.multiply(getClosedAmountMultiplier(closedPosition)))
                    .orElse(null);
            if (openPrice != null && closePrice != null) {
                // если ценная бумага не вводилась и не выводилась со счета, а была куплена и продана
                // (есть цены покупки и продажи)
                purchaseCost = purchaseCost.add(openPrice).add(closePrice);
            } else if (openPrice != null && closedPosition.getClosingEvent() == CashFlowType.REDEMPTION) {
                // Событие погашение не имеет цену закрытия (событие CashFlowType.PRICE), учитываем цену открытия,
                // цена закрытия будет учтена ниже из объектов 'SecurityEventCashFlow'
                purchaseCost = purchaseCost.add(openPrice);
            }
        }
        return positions.getRedemptions()
                .stream()
                .map(entity -> convertToCurrency(entity.getValue(), entity.getCurrency(), toCurrency))
                .map(BigDecimal::abs)
                .reduce(purchaseCost, BigDecimal::add);
    }

    @Override
    public BigDecimal getPurchaseAccruedInterest(Security security, FifoPositions positions, String toCurrency) {
        if (security.getType().isBond()) {
            return getTotal(positions.getTransactions(), CashFlowType.ACCRUED_INTEREST, toCurrency);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotal(Deque<Transaction> transactions, CashFlowType type, String toCurrency) {
        return transactions.stream()
                .filter(t -> t.getId() != null && t.getCount() != 0)
                .map(t -> getTransactionValue(t, type, toCurrency))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<BigDecimal> getTransactionValue(Transaction t, CashFlowType type, String toCurrency) {
        if (t.getId() == null) { // redemption
            return Optional.empty();
        }
        return transactionCashFlowRepository
                .findByTransactionIdAndCashFlowType(t.getId(), type)
                .map(entity -> convertToCurrency(entity.getValue(), entity.getCurrency(), toCurrency));
    }

    private BigDecimal getOpenAmountMultiplier(OpenedPosition openedPosition) {
        int positionCount = Math.abs(openedPosition.getCount());
        int transactionCount = Math.abs(openedPosition.getOpenTransaction().getCount());
        if (positionCount == transactionCount) {
            return BigDecimal.ONE;
        } else {
            return BigDecimal.valueOf(positionCount)
                    .divide(BigDecimal.valueOf(transactionCount), 6, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal getClosedAmountMultiplier(ClosedPosition closedPosition) {
        int positionCount = Math.abs(closedPosition.getCount());
        int transactionCount = Math.abs(closedPosition.getCloseTransaction().getCount());
        if (positionCount == transactionCount) {
            return BigDecimal.ONE;
        } else {
            return BigDecimal.valueOf(positionCount)
                    .divide(BigDecimal.valueOf(transactionCount), 6, RoundingMode.HALF_UP);
        }
    }

    @Override
    public BigDecimal sumPaymentsForType(Collection<String> portfolios, Security security, CashFlowType cashFlowType, String toCurrency) {
        return getSecurityEventCashFlowEntities(portfolios, security, cashFlowType)
                .stream()
                .map(entity -> convertToCurrency(entity.getValue(), entity.getCurrency(), toCurrency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<SecurityEventCashFlowEntity> getSecurityEventCashFlowEntities(Collection<String> portfolios,
                                                                               Security security,
                                                                               CashFlowType cashFlowType) {
        return portfolios.isEmpty() ?
                securityEventCashFlowRepository
                        .findBySecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                                security.getId(),
                                cashFlowType.getId(),
                                ViewFilter.get().getFromDate(),
                                ViewFilter.get().getToDate()) :
                securityEventCashFlowRepository
                        .findByPortfolioIdInAndSecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                                portfolios,
                                security.getId(),
                                cashFlowType.getId(),
                                ViewFilter.get().getFromDate(),
                                ViewFilter.get().getToDate());
    }

    @Override
    public SecurityQuote getSecurityQuote(Security security, String toCurrency, Instant to) {
        if (security.getType() == CURRENCY_PAIR) {
            String currencyPair = securityRepository.findCurrencyPair(security.getId()).orElseThrow();
            String baseCurrency = currencyPair.substring(0, 3);
            LocalDate toDate = LocalDate.ofInstant(to, ZoneId.systemDefault());
            BigDecimal lastPrice = toDate.isBefore(LocalDate.now()) ?
                    foreignExchangeRateService.getExchangeRateOrDefault(baseCurrency, toCurrency, toDate) :
                    foreignExchangeRateService.getExchangeRate(baseCurrency, toCurrency);
            return SecurityQuote.builder()
                    .security(security.getId())
                    .timestamp(to)
                    .quote(lastPrice)
                    .currency(toCurrency)
                    .build();
        }
        return securityQuoteRepository
                .findFirstBySecurityIdAndTimestampLessThanOrderByTimestampDesc(security.getId(), to)
                .map(securityQuoteConverter::fromEntity)
                .map(_quote -> foreignExchangeRateService.convertQuoteToCurrency(_quote, toCurrency, security.getType()))
                .map(_quote -> hasLength(_quote.getCurrency()) ? _quote : _quote.toBuilder()
                        .currency(toCurrency) // Не известно точно в какой валюте котируется инструмент,
                        .build())             // делаем предположение, что в валюте сделки
                .orElse(null);
    }

    @Override
    public Optional<BigDecimal> getSecurityQuoteFromLastTransaction(Security security, String toCurrency) {
        return transactionRepository.findFirstBySecurityIdOrderByTimestampDesc(security.getId())
                .map(t -> transactionCashFlowRepository
                        .findByTransactionIdAndCashFlowTypeIn(t.getId(), priceAndAccruedInterestTypes)
                        .stream()
                        .map(cashFlow -> cashFlow.getValue()
                                .divide(BigDecimal.valueOf(t.getCount()), 2, RoundingMode.HALF_UP)
                                .multiply(foreignExchangeRateService.getExchangeRate(cashFlow.getCurrency(), toCurrency))
                                .abs())
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .map(value -> Objects.equals(value, BigDecimal.ZERO) ? null : value);
    }

    private BigDecimal convertToCurrency(BigDecimal value, String fromCurrency, String toCurrency) {
        return foreignExchangeRateService.convertValueToCurrency(value, fromCurrency, toCurrency);
    }
}

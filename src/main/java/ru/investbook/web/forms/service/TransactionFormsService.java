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

package ru.investbook.web.forms.service;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.AbstractTransaction.AbstractTransactionBuilder;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.converter.TransactionCashFlowConverter;
import ru.investbook.converter.TransactionConverter;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.web.forms.model.SecurityType;
import ru.investbook.web.forms.model.TransactionModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.util.Optional.ofNullable;
import static ru.investbook.web.forms.model.SecurityType.DERIVATIVE;

@Component
@RequiredArgsConstructor
public class TransactionFormsService implements FormsService<TransactionModel> {
    private static final ZoneId zoneId = ZoneId.systemDefault();
    private final TransactionRepository transactionRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final PortfolioRepository portfolioRepository;
    private final TransactionCashFlowConverter transactionCashFlowConverter;
    private final TransactionConverter transactionConverter;
    private final PortfolioConverter portfolioConverter;
    private final SecurityRepositoryHelper securityRepositoryHelper;
    private final Set<Integer> cashFlowTypes = Set.of(CashFlowType.PRICE.getId(),
            CashFlowType.ACCRUED_INTEREST.getId(),
            CashFlowType.DERIVATIVE_QUOTE.getId(),
            CashFlowType.DERIVATIVE_PRICE.getId(),
            CashFlowType.COMMISSION.getId());

    @Transactional(readOnly = true)
    public Optional<TransactionModel> getById(int id) {
        return transactionRepository.findById(id)
                .map(this::toTransactionModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionModel> getAll() {
        Set<String> activePortfolios = portfolioRepository.findByEnabledIsTrue()
                .stream()
                .map(PortfolioEntity::getId)
                .collect(Collectors.toSet());
        return transactionRepository
                .findByPortfolioInOrderByPortfolioAscTimestampDescSecurityIdAsc(activePortfolios)
                .stream()
                .map(this::toTransactionModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void save(TransactionModel tr) {
        int savedSecurityId = securityRepositoryHelper.saveAndFlushSecurity(tr);
        int direction = ((tr.getAction() == TransactionModel.Action.BUY) ? 1 : -1);
        BigDecimal multiplier = BigDecimal.valueOf(-direction * tr.getCount());

        AbstractTransactionBuilder<?, ?> builder;

        if (tr.getPrice() != null) {
            builder = switch (tr.getSecurityType()) {
                case SHARE, BOND, ASSET -> SecurityTransaction.builder()
                        .value(tr.getPrice().multiply(multiplier))
                        .valueCurrency(tr.getPriceCurrency())
                        .accruedInterest(ofNullable(tr.getAccruedInterest())
                                .map(v -> v.multiply(multiplier))
                                .orElse(null));
                case DERIVATIVE -> {
                    BigDecimal value = null;
                    BigDecimal valueInPoints = tr.getPrice().multiply(multiplier);
                    if (tr.hasDerivativeTickValue()) {
                        value = valueInPoints
                                .multiply(tr.getPriceTickValue())
                                .divide(tr.getPriceTick(), 6, RoundingMode.HALF_UP);
                    }
                    yield DerivativeTransaction.builder()
                            .valueInPoints(valueInPoints)
                            .value(value)
                            .valueCurrency(tr.getPriceTickValueCurrency());
                }
                case CURRENCY -> ForeignExchangeTransaction.builder()
                        .value(tr.getPrice().multiply(multiplier))
                        .valueCurrency(tr.getPriceCurrency());
            };

            if (tr.getCommission() != null) {
                builder
                        .commission(tr.getCommission().negate())
                        .commissionCurrency(tr.getCommissionCurrency());
            }
        } else {
            builder = switch (tr.getSecurityType()) {
                case SHARE, BOND -> SecurityTransaction.builder();
                default -> throw new IllegalArgumentException("Only bond and stock can have deposit and withdrawal events");
            };
        }

        AbstractTransaction transaction = builder
                .id(tr.getId())
                .tradeId(tr.getTradeId())
                .portfolio(tr.getPortfolio())
                .timestamp(tr.getDate().atStartOfDay(zoneId).toInstant())
                .security(savedSecurityId)
                .count(abs(tr.getCount()) * direction)
                .build();

        saveAndFlush(tr, transaction);
    }

    private void saveAndFlush(TransactionModel transactionModel,
                              AbstractTransaction transaction) {
        saveAndFlush(transactionModel.getPortfolio());
        TransactionEntity transactionEntity = transactionRepository.saveAndFlush(
                transactionConverter.toEntity(transaction.getTransaction()));
        transactionModel.setId(transactionEntity.getId()); // used by view
        Optional.ofNullable(transactionEntity.getId()).ifPresent(transactionCashFlowRepository::deleteByTransactionId);
        transactionCashFlowRepository.flush();
        transaction.toBuilder()
                .id(transactionEntity.getId())
                .build()
                .getTransactionCashFlows()
                .stream()
                .map(transactionCashFlowConverter::toEntity)
                .forEach(transactionCashFlowRepository::save);
    }

    private void saveAndFlush(String portfolio) {
        if (!portfolioRepository.existsById(portfolio)) {
            portfolioRepository.saveAndFlush(
                    portfolioConverter.toEntity(Portfolio.builder()
                            .id(portfolio)
                            .build()));
        }
    }

    private TransactionModel toTransactionModel(TransactionEntity e) {
        TransactionModel m = new TransactionModel();
        m.setId(e.getId());
        m.setTradeId(e.getTradeId());
        m.setPortfolio(e.getPortfolio());
        int count = e.getCount();
        BigDecimal cnt = BigDecimal.valueOf(count);
        m.setAction(count >= 0 ? TransactionModel.Action.BUY : TransactionModel.Action.CELL);
        m.setDate(e.getTimestamp().atZone(zoneId).toLocalDate());
        SecurityEntity securityEntity = e.getSecurity();
        AtomicReference<SecurityType> securityType = new AtomicReference<>(SecurityType.valueOf(securityEntity.getType()));
        m.setCount(abs(count));
        List<TransactionCashFlowEntity> cashFlows = transactionCashFlowRepository.findByTransactionIdAndCashFlowTypeIn(
                e.getId(),
                cashFlowTypes);
        cashFlows.forEach(value -> {
            CashFlowType type = CashFlowType.valueOf(value.getCashFlowType().getId());
            switch (type) {
                case PRICE, DERIVATIVE_QUOTE -> {
                    m.setPrice(value.getValue().divide(cnt, 6, RoundingMode.HALF_UP).abs());
                    m.setPriceCurrency(value.getCurrency());
                    if (type == CashFlowType.DERIVATIVE_QUOTE) {
                        securityType.set(DERIVATIVE);
                    }
                }
                case ACCRUED_INTEREST -> {
                    m.setAccruedInterest(value.getValue().divide(cnt, 6, RoundingMode.HALF_UP).abs());
                    securityType.set(SecurityType.BOND);
                }
                case COMMISSION -> {
                    m.setCommission(value.getValue().abs());
                    m.setCommissionCurrency(value.getCurrency());
                }
            }
        });
        m.setSecurity(
                securityEntity.getIsin(),
                ofNullable(securityEntity.getName()).orElse(securityEntity.getTicker()),
                securityType.get());
        if (m.getSecurityType() == DERIVATIVE &&
                m.getPrice() != null && m.getPrice().floatValue() > 0.000001) {
            cashFlows.stream()
                    .filter(value -> CashFlowType.valueOf(value.getCashFlowType().getId()) == CashFlowType.DERIVATIVE_PRICE)
                    .forEach(value -> {
                        m.setPriceTick(BigDecimal.ONE); // information not stored in db, normalizing
                        m.setPriceTickValue(value.getValue()
                                .divide(BigDecimal.valueOf(m.getCount()), 6, RoundingMode.HALF_UP)
                                .divide(m.getPrice(), 6, RoundingMode.HALF_UP)
                                .abs());
                        m.setPriceTickValueCurrency(value.getCurrency());
                    });
        }
        return m;
    }

    @Transactional
    public void delete(int transactionId) {
        transactionRepository.deleteById(transactionId);
        transactionRepository.flush();
    }
}

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

package ru.investbook.web.forms.service;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.AbstractTransaction.AbstractTransactionBuilder;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.converter.TransactionCashFlowConverter;
import ru.investbook.converter.TransactionConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.entity.TransactionEntity_;
import ru.investbook.report.FifoPositions;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.report.FifoPositionsFilter;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.repository.specs.SecurityDepositSearchSpecification;
import ru.investbook.repository.specs.TransactionSearchSpecification;
import ru.investbook.web.forms.model.SecurityType;
import ru.investbook.web.forms.model.SplitModel;
import ru.investbook.web.forms.model.TransactionModel;
import ru.investbook.web.forms.model.filter.TransactionFormFilterModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Math.abs;
import static java.util.Optional.ofNullable;
import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;
import static ru.investbook.web.forms.model.SecurityType.DERIVATIVE;

@Component
@RequiredArgsConstructor
public class TransactionFormsService {
    private static final ZoneId zoneId = ZoneId.systemDefault();
    private final TransactionRepository transactionRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final PortfolioRepository portfolioRepository;
    private final TransactionCashFlowConverter transactionCashFlowConverter;
    private final TransactionConverter transactionConverter;
    private final PortfolioConverter portfolioConverter;
    private final SecurityRepositoryHelper securityRepositoryHelper;
    private final FifoPositionsFactory fifoPositionsFactory;
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

    @Transactional(readOnly = true)
    public Page<TransactionModel> getTransactionPage(TransactionFormFilterModel filter) {
        TransactionSearchSpecification spec = TransactionSearchSpecification.of(
                filter.getPortfolio(), filter.getSecurity(), filter.getDateFrom(), filter.getDateTo());

        return getTransactionModels(spec, filter);
    }

    @Transactional(readOnly = true)
    public Page<TransactionModel> getSecurityDepositPage(TransactionFormFilterModel filter) {
        SecurityDepositSearchSpecification spec = SecurityDepositSearchSpecification.of(
                filter.getPortfolio(), filter.getSecurity(), filter.getDateFrom(), filter.getDateTo());

        return getTransactionModels(spec, filter);
    }

    @NonNull
    private Page<TransactionModel> getTransactionModels(Specification<TransactionEntity> spec,
                                                        TransactionFormFilterModel filter) {
        Sort sort = Sort.by(asc(TransactionEntity_.PORTFOLIO), desc(TransactionEntity_.TIMESTAMP), asc("security.id"));
        PageRequest page = PageRequest.of(filter.getPage(), filter.getPageSize(), sort);

        return transactionRepository.findAll(spec, page)
                .map(this::toTransactionModel);
    }

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
                default ->
                        throw new IllegalArgumentException("Only bond and stock can have deposit and withdrawal events");
            };
        }

        AbstractTransaction transaction = builder
                .id(tr.getId())
                .tradeId(tr.getTradeId())
                .portfolio(tr.getPortfolio())
                .timestamp(tr.getDate().atTime(tr.getTime()).atZone(zoneId).toInstant())
                .security(savedSecurityId)
                .count(abs(tr.getCount()) * direction)
                .build();

        saveAndFlush(tr.getPortfolio());
        int transactionId = saveAndFlush(transaction);
        tr.setId(transactionId); // used by view
    }

    /**
     * @return saved transaction id
     */
    private int saveAndFlush(AbstractTransaction transaction) {
        TransactionEntity transactionEntity = transactionRepository.saveAndFlush(
                transactionConverter.toEntity(transaction.getTransaction()));

        Optional.ofNullable(transactionEntity.getId()).ifPresent(transactionCashFlowRepository::deleteByTransactionId);
        transactionCashFlowRepository.flush();
        transaction.toBuilder()
                .id(transactionEntity.getId())
                .build()
                .getTransactionCashFlows()
                .stream()
                .map(transactionCashFlowConverter::toEntity)
                .forEach(transactionCashFlowRepository::save);
        return transactionEntity.getId();
    }

    private void saveAndFlush(String portfolio) {
        if (!portfolioRepository.existsById(portfolio)) {
            portfolioRepository.saveAndFlush(
                    portfolioConverter.toEntity(Portfolio.builder()
                            .id(portfolio)
                            .build()));
        }
    }

    public void save(SplitModel split) {
        int savedSecurityId = securityRepositoryHelper.saveAndFlushSecurity(split);

        Instant splitInstant = split.getDate().atTime(split.getTime()).atZone(zoneId).toInstant();
        checkWithdrawalCount(split, savedSecurityId, splitInstant);

        SecurityTransaction.SecurityTransactionBuilder<?, ?> builder = SecurityTransaction.builder()
                .portfolio(split.getPortfolio())
                .timestamp(splitInstant)
                .security(savedSecurityId);

        saveAndFlush(split.getPortfolio());
        saveAndFlush(builder
                .tradeId(split.getTradeId(savedSecurityId) + "w")
                .count(-Math.abs(split.getWithdrawalCount()))
                .build());
        saveAndFlush(builder
                .tradeId(split.getTradeId(savedSecurityId) + "d")
                .count(Math.abs(split.getDepositCount()))
                .build());
    }

    private void checkWithdrawalCount(SplitModel split, int savedSecurityId, Instant splitInstant) {
        FifoPositions positions = fifoPositionsFactory.get(savedSecurityId,
                org.spacious_team.broker.pojo.SecurityType.STOCK,
                FifoPositionsFilter.of(split.getPortfolio(), Instant.EPOCH, splitInstant));
        Assert.isTrue(positions.getCurrentOpenedPositionsCount() == Math.abs(split.getWithdrawalCount()),
                () -> "На момент сплита " + split.getDate() + " в " + split.getTime() +
                        " на счету '" + split.getPortfolio() + "' " +
                        "находилось " + positions.getCurrentOpenedPositionsCount() + " акций " + split.getSecurity() +
                        ", вы указали другое количество");
    }

    private TransactionModel toTransactionModel(TransactionEntity e) {
        TransactionModel m = new TransactionModel();
        m.setId(e.getId());
        m.setTradeId(e.getTradeId());
        m.setPortfolio(e.getPortfolio());
        int count = e.getCount();
        BigDecimal cnt = BigDecimal.valueOf(count);
        m.setAction(count >= 0 ? TransactionModel.Action.BUY : TransactionModel.Action.CELL);
        ZonedDateTime zonedDateTime = e.getTimestamp().atZone(zoneId);
        m.setDate(zonedDateTime.toLocalDate());
        m.setTime(zonedDateTime.toLocalTime());
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

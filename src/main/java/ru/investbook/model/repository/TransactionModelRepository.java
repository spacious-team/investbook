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

package ru.investbook.model.repository;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.pojo.Transaction;
import org.spacious_team.broker.pojo.TransactionCashFlow;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.AbstractTransaction.AbstractTransactionBuilder;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.converter.TransactionCashFlowConverter;
import ru.investbook.converter.TransactionConverter;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.entity.TransactionEntityPK;
import ru.investbook.model.dto.TransactionModel;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.util.Optional.ofNullable;

@Component
@RequiredArgsConstructor
public class TransactionModelRepository implements ModelRepository<TransactionModel> {
    private static final ZoneId zoneId = ZoneId.systemDefault();
    private final TransactionRepository transactionRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final SecurityRepository securityRepository;
    private final PortfolioRepository portfolioRepository;
    private final TransactionCashFlowConverter transactionCashFlowConverter;
    private final TransactionConverter transactionConverter;
    private final SecurityConverter securityConverter;
    private final PortfolioConverter portfolioConverter;
    private final Set<Integer> cashFlowTypes = Set.of(CashFlowType.PRICE.getId(),
            CashFlowType.ACCRUED_INTEREST.getId(),
            CashFlowType.DERIVATIVE_QUOTE.getId(),
            CashFlowType.DERIVATIVE_PRICE.getId(),
            CashFlowType.COMMISSION.getId());

    public Optional<TransactionModel> findById(String portfolio, String transactionId) {
        TransactionEntityPK pk = new TransactionEntityPK();
        pk.setId(transactionId);
        pk.setPortfolio(portfolio);
        return transactionRepository.findById(pk)
                .map(this::toTransactionModel);
    }

    @Override
    public List<TransactionModel> findAll() {
        return transactionRepository.findByOrderByPkPortfolioAscTimestampDescSecurityIdAsc()
                .stream()
                .map(this::toTransactionModel)
                .collect(Collectors.toList());
    }

    @Override
    public void saveAndFlush(TransactionModel tr) {
        int direction = ((tr.getAction() == TransactionModel.Action.BUY) ? 1 : -1);
        BigDecimal multiplier = BigDecimal.valueOf(-direction * tr.getCount());

        AbstractTransactionBuilder<?, ?> builder = switch (tr.getSecurityType()) {
            case SHARE, BOND -> SecurityTransaction.builder()
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

        AbstractTransaction transaction = builder.portfolio(tr.getPortfolio())
                .transactionId(tr.getTransactionId())
                .timestamp(tr.getDate().atStartOfDay(zoneId).toInstant())
                .security(tr.getSecurityId())
                .count(abs(tr.getCount()) * direction)
                .commission(tr.getCommission().negate())
                .commissionCurrency(tr.getCommissionCurrency())
                .build();

        saveAndFlush(tr, transaction.getTransaction(), transaction.getTransactionCashFlows());
    }

    private void saveAndFlush(TransactionModel transactionModel,
                              Transaction transaction,
                              Collection<TransactionCashFlow> cashFlows) {
        if (!portfolioRepository.existsById(transaction.getPortfolio())) {
            portfolioRepository.saveAndFlush(
                    portfolioConverter.toEntity(Portfolio.builder()
                            .id(transaction.getPortfolio())
                            .build()));
        }
        String securityId = transactionModel.getSecurityId();
        if (!securityRepository.existsById(securityId)) {
            securityRepository.saveAndFlush(
                    securityConverter.toEntity(Security.builder()
                            .id(securityId)
                            .name(transactionModel.getSecurityName())
                            .build()));
        }
        transactionRepository.saveAndFlush(transactionConverter.toEntity(transaction));
        transactionCashFlowRepository.deleteByPkPortfolioAndPkTransactionId(transaction.getPortfolio(), transaction.getId());
        cashFlows.forEach(cash -> transactionCashFlowRepository.save(transactionCashFlowConverter.toEntity(cash)));
        transactionCashFlowRepository.flush();
    }

    private TransactionModel toTransactionModel(TransactionEntity e) {
        TransactionModel m = new TransactionModel();
        m.setTransactionId(e.getPk().getId());
        m.setPortfolio(e.getPk().getPortfolio());
        int count = e.getCount();
        BigDecimal cnt = BigDecimal.valueOf(count);
        m.setAction(count >= 0 ? TransactionModel.Action.BUY : TransactionModel.Action.CELL);
        m.setDate(e.getTimestamp().atZone(zoneId).toLocalDate());
        m.setSecurity(e.getSecurity().getId(), e.getSecurity().getName());
        m.setSecurityType(SecurityType.getSecurityType(e.getSecurity().getId()));
        m.setCount(abs(count));
        List<TransactionCashFlowEntity> cashFlows = transactionCashFlowRepository.findByPkPortfolioAndPkTransactionIdAndPkTypeIn(
                e.getPk().getPortfolio(),
                e.getPk().getId(),
                cashFlowTypes);
        cashFlows.forEach(value -> {
                    CashFlowType type = CashFlowType.valueOf(value.getCashFlowType().getId());
                    switch (type) {
                        case PRICE, DERIVATIVE_QUOTE -> {
                            m.setPrice(value.getValue().divide(cnt, 6, RoundingMode.HALF_UP).abs());
                            m.setPriceCurrency(value.getCurrency());
                            if (type == CashFlowType.DERIVATIVE_QUOTE) {
                                m.setSecurityType(TransactionModel.SecurityType.DERIVATIVE);
                            }
                        }
                        case ACCRUED_INTEREST -> {
                            m.setAccruedInterest(value.getValue().divide(cnt, 6, RoundingMode.HALF_UP).abs());
                            m.setSecurityType(TransactionModel.SecurityType.BOND);
                        }
                        case COMMISSION -> {
                            m.setCommission(value.getValue().abs());
                            m.setCommissionCurrency(value.getCurrency());
                        }
                    }
                });
        if (m.getSecurityType() == TransactionModel.SecurityType.DERIVATIVE &&
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
}

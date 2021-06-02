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

package ru.investbook.report.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.stereotype.Component;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.service.SecurityProfitService;
import ru.investbook.service.moex.MoexDerivativeCodeService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;
import static org.spacious_team.broker.pojo.CashFlowType.DERIVATIVE_PROFIT;
import static ru.investbook.report.excel.DerivativesMarketTotalProfitExcelTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DerivativesMarketTotalProfitExcelTableFactory implements TableFactory {
    private static final String QUOTE_CURRENCY = "PNT"; // point
    private static final String PROFIT_FORMULA = getProfitFormula();
    private static final String PROFIT_PROPORTION_FORMULA = getProfitProportionFormula();
    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final FifoPositionsFactory positionsFactory;
    private final ForeignExchangeRateService foreignExchangeRateService;
    private final Set<Integer> paymentEvents = Set.of(DERIVATIVE_PROFIT.getId());
    private final MoexDerivativeCodeService moexDerivativeCodeService;
    private final SecurityProfitService securityProfitService;

    public Table create(Portfolio portfolio) {
        throw new UnsupportedOperationException();
    }

    public Table create(Portfolio portfolio, String forCurrency) {
        return create(singleton(portfolio.getId()), forCurrency);
    }

    /**
     * @param portfolios should be empty for display for all
     */
    @Override
    public Table create(Collection<String> portfolios, String forCurrency) {
        Collection<String> contractGroups = getContractGroups(portfolios, forCurrency);
        return create(portfolios, contractGroups, forCurrency);
    }

    private Table create(Collection<String> portfolios, Collection<String> contractGroups, String forCurrency) {
        return contractGroups.stream()
                .map(group -> getSecurityStatus(portfolios, group, forCurrency))
                .collect(toCollection(Table::new));
    }

    private Collection<String> getContractGroups(Collection<String> portfolios, String currency) {
        if (!currency.equalsIgnoreCase("RUB")) {
            return emptyList();
        }
        Collection<String> contracts = portfolios.isEmpty() ?
                transactionRepository.findDistinctDerivativeByTimestampBetweenOrderByTimestampDesc(
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate()) :
                transactionRepository.findDistinctDerivativeByPortfolioInAndTimestampBetweenOrderByTimestampDesc(
                        portfolios,
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate());

        return contracts.stream()
                .map(moexDerivativeCodeService::getContractGroup)
                .flatMap(Optional::stream)
                .sorted()
                .collect(toCollection(LinkedHashSet::new));
    }

    private Set<String> getContracts(String contractGroup) {
        return securityRepository.findAll()
                .stream()
                .filter(security -> belongsToContractGroup(security, contractGroup))
                .map(SecurityEntity::getId)
                .collect(toSet());
    }

    private boolean belongsToContractGroup(SecurityEntity security, String expectedGroup) {
        return moexDerivativeCodeService.getContractGroup(security.getId())
                .map(expectedGroup::equals)
                .orElse(false);
    }

    private Table.Record getSecurityStatus(Collection<String> portfolios, String contractGroup, String toCurrency) {
        Table.Record row = new Table.Record();
        try {
            Set<String> contracts = getContracts(contractGroup);
            Deque<Transaction> transactions = getTransactions(portfolios, contracts);

            row.put(CONTRACT_GROUP, moexDerivativeCodeService.codePrefixToShortnamePrefix(contractGroup)
                    .orElse(contractGroup));
            row.put(FIRST_TRANSACTION_DATE, ofNullable(transactions.peekFirst())
                    .map(Transaction::getTimestamp)
                    .orElse(null));
            row.put(LAST_TRANSACTION_DATE, ofNullable(transactions.peekLast())
                    .map(Transaction::getTimestamp)
                    .orElse(null));
            row.put(LAST_EVENT_DATE, getLastEventDate(portfolios, contracts));
            row.put(BUY_COUNT, transactions
                    .stream()
                    .mapToInt(Transaction::getCount)
                    .filter(count -> count > 0)
                    .sum());
            row.put(CELL_COUNT, Math.abs(transactions
                    .stream()
                    .mapToInt(Transaction::getCount)
                    .filter(count -> count < 0)
                    .sum()));

            Map<String, Integer> contractToOpenedPositions = transactions.stream()
                    .collect(groupingBy(Transaction::getSecurity, summingInt(Transaction::getCount)));
            int openedPositions = contractToOpenedPositions.values().stream().mapToInt(Math::abs).sum();

            row.put(COUNT, openedPositions);
            row.put(COMMISSION, getTotal(transactions, CashFlowType.COMMISSION, toCurrency).abs());
            if (openedPositions == 0) {
                row.put(GROSS_PROFIT_PNT, getTotal(transactions, CashFlowType.DERIVATIVE_QUOTE, QUOTE_CURRENCY));
            }
            row.put(GROSS_PROFIT, getGrossProfit(portfolios, contracts, toCurrency));
            row.put(PROFIT, PROFIT_FORMULA);
            row.put(PROFIT_PROPORTION, PROFIT_PROPORTION_FORMULA);
        } catch (Exception e) {
            log.error("Ошибка при формировании агрегированных данных по группе контрактов {}", contractGroup, e);
        }
        return row;
    }

    private Deque<Transaction> getTransactions(Collection<String> portfolios, Set<String> contracts) {
        ViewFilter filter = ViewFilter.get();
        return contracts.stream()
                .map(contract -> positionsFactory.getTransactions(portfolios, contract, filter))
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(Transaction::getTimestamp))
                .collect(toCollection(LinkedList::new));
    }

    private Instant getLastEventDate(Collection<String> portfolios, Collection<String> contracts) {
        ViewFilter filter = ViewFilter.get();
        return contracts.stream()
                .map(contract -> getLastEventDate(portfolios, contract, filter))
                .flatMap(Optional::stream)
                .map(SecurityEventCashFlowEntity::getTimestamp)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private Optional<SecurityEventCashFlowEntity> getLastEventDate(Collection<String> portfolios,
                                                                   String contract, ViewFilter filter) {
        return portfolios.isEmpty() ?
                securityEventCashFlowRepository
                        .findFirstBySecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
                                contract, paymentEvents, filter.getFromDate(), filter.getToDate()) :
                securityEventCashFlowRepository
                        .findFirstByPortfolioIdInAndSecurityIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
                                portfolios, contract, paymentEvents, filter.getFromDate(), filter.getToDate());
    }

    /**
     * Cуммарная вариационная маржа по всем контрактам
     */
    private BigDecimal getGrossProfit(Collection<String> portfolios, Collection<String> contracts, String toCurrency) {
        return contracts.stream()
                .map(contract -> sumDerivativeProfitPayments(portfolios, contract, toCurrency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getTotal(Deque<Transaction> transactions, CashFlowType type, String toCurrency) {
        return transactions.stream()
                .filter(t -> t.getId() != null && t.getCount() != 0)
                .map(t -> getTransactionValue(t, type, toCurrency))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<BigDecimal> getTransactionValue(Transaction t, CashFlowType type, String toCurrency) {
        return transactionCashFlowRepository
                .findByPkPortfolioAndPkTransactionIdAndPkType(t.getPortfolio(), t.getId(), type.getId())
                .map(entity -> convertToCurrency(entity.getValue(), entity.getCurrency(), toCurrency));
    }

    private BigDecimal sumDerivativeProfitPayments(Collection<String> portfolios, String contract, String toCurrency) {
        return securityProfitService.getSecurityEventCashFlowEntities(portfolios, contract, DERIVATIVE_PROFIT)
                .stream()
                .map(entity -> convertToCurrency(entity.getValue(), entity.getCurrency(), toCurrency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal convertToCurrency(BigDecimal value, String fromCurrency, String toCurrency) {
        return foreignExchangeRateService.convertValueToCurrency(value, fromCurrency, toCurrency);
    }


    private static String getProfitFormula() {
        return "=" + GROSS_PROFIT.getCellAddr() + "-" + COMMISSION.getCellAddr();
    }

    private static String getProfitProportionFormula() {
        return "=" + PROFIT.getCellAddr() + "/ABS(" + PROFIT.getColumnIndex() + "2)";
    }
}

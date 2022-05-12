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

package ru.investbook.report.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.stereotype.Component;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.report.FifoPositionsFilter;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.SecurityRepository;
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
import static org.spacious_team.broker.pojo.CashFlowType.DERIVATIVE_QUOTE;
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
    private final SecurityConverter securityConverter;
    private final FifoPositionsFactory positionsFactory;
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
        ViewFilter filter = ViewFilter.get();
        Collection<Integer> contracts = portfolios.isEmpty() ?
                transactionRepository.findDistinctDerivativeByTimestampBetweenOrderByTimestampDesc(
                        filter.getFromDate(),
                        filter.getToDate()) :
                transactionRepository.findDistinctDerivativeByPortfolioInAndTimestampBetweenOrderByTimestampDesc(
                        portfolios,
                        filter.getFromDate(),
                        filter.getToDate());

        return contracts.stream()
                .map(securityRepository::findById)
                .flatMap(Optional::stream)
                .map(SecurityEntity::getTicker)
                .map(moexDerivativeCodeService::getContractGroup)
                .flatMap(Optional::stream)
                .sorted()
                .collect(toCollection(LinkedHashSet::new));
    }

    private Set<Security> getContracts(String contractGroup) {
        return securityRepository.findByType(SecurityType.DERIVATIVE)
                .stream()
                .filter(security -> belongsToContractGroup(security, contractGroup))
                .map(securityConverter::fromEntity)
                .collect(toSet());
    }

    private boolean belongsToContractGroup(SecurityEntity security, String expectedGroup) {
        return moexDerivativeCodeService.getContractGroup(security.getTicker())
                .map(expectedGroup::equals)
                .orElse(false);
    }

    private Table.Record getSecurityStatus(Collection<String> portfolios, String contractGroup, String toCurrency) {
        Table.Record row = new Table.Record();
        try {
            Set<Security> contracts = getContracts(contractGroup);
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

            Map<Integer, Integer> contractToOpenedPositions = transactions.stream()
                    .collect(groupingBy(Transaction::getSecurity, summingInt(Transaction::getCount)));
            int openedPositions = contractToOpenedPositions.values().stream().mapToInt(Math::abs).sum();

            row.put(COUNT, openedPositions);
            row.put(COMMISSION, securityProfitService.getTotal(transactions, CashFlowType.COMMISSION, toCurrency).abs());
            if (openedPositions == 0) {
                row.put(GROSS_PROFIT_PNT, securityProfitService.getTotal(transactions, DERIVATIVE_QUOTE, QUOTE_CURRENCY));
            }
            row.put(GROSS_PROFIT, getGrossProfit(portfolios, contracts, toCurrency));
            row.put(PROFIT, PROFIT_FORMULA);
            row.put(PROFIT_PROPORTION, PROFIT_PROPORTION_FORMULA);
        } catch (Exception e) {
            log.error("Ошибка при формировании агрегированных данных по группе контрактов {}", contractGroup, e);
        }
        return row;
    }

    private Deque<Transaction> getTransactions(Collection<String> portfolios, Set<Security> contracts) {
        ViewFilter filter = ViewFilter.get();
        FifoPositionsFilter pf = FifoPositionsFilter.of(portfolios, filter.getFromDate(), filter.getToDate());
        return contracts.stream()
                .map(contract -> positionsFactory.getTransactions(contract.getId(), pf))
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(Transaction::getTimestamp))
                .collect(toCollection(LinkedList::new));
    }

    private Instant getLastEventDate(Collection<String> portfolios, Collection<Security> contracts) {
        ViewFilter filter = ViewFilter.get();
        return contracts.stream()
                .map(contract -> securityProfitService.getLastEventTimestamp(
                        portfolios, contract, paymentEvents, filter.getFromDate(), filter.getToDate()))
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * Суммарная вариационная маржа по всем контрактам
     */
    private BigDecimal getGrossProfit(Collection<String> portfolios, Collection<Security> contracts, String toCurrency) {
        return contracts.stream()
                .map(contract -> securityProfitService.sumPaymentsForType(portfolios, contract, DERIVATIVE_PROFIT, toCurrency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String getProfitFormula() {
        return "=" + GROSS_PROFIT.getCellAddr() + "-" + COMMISSION.getCellAddr();
    }

    private static String getProfitProportionFormula() {
        return "=" + PROFIT.getCellAddr() + "/ABS(" + PROFIT.getColumnIndex() + "2)";
    }
}

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

package ru.investbook.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.Transaction;
import org.spacious_team.broker.pojo.TransactionCashFlow;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.investbook.api.EventCashFlowRestController;
import ru.investbook.api.ForeignExchangeRateRestController;
import ru.investbook.api.PortfolioPropertyRestController;
import ru.investbook.api.PortfolioRestController;
import ru.investbook.api.SecurityEventCashFlowRestController;
import ru.investbook.api.SecurityQuoteRestController;
import ru.investbook.api.SecurityRestController;
import ru.investbook.api.TransactionCashFlowRestController;
import ru.investbook.api.TransactionRestController;

import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static ru.investbook.repository.RepositoryHelper.isUniqIndexViolationException;

@Component
@Slf4j
@RequiredArgsConstructor
public class InvestbookApiClient {
    private final PortfolioRestController portfolioRestController;
    private final SecurityRestController securityRestController;
    private final SecurityEventCashFlowRestController securityEventCashFlowRestController;
    private final EventCashFlowRestController eventCashFlowRestController;
    private final TransactionRestController transactionRestController;
    private final TransactionCashFlowRestController transactionCashFlowRestController;
    private final PortfolioPropertyRestController portfolioPropertyRestController;
    private final SecurityQuoteRestController securityQuoteRestController;
    private final ForeignExchangeRateRestController foreignExchangeRateRestController;

    public boolean addPortfolio(Portfolio portfolio) {
        return handlePost(
                () -> portfolioRestController.post(portfolio),
                "Не могу сохранить Портфель " + portfolio);
    }

    public boolean addSecurity(String security) {
        return addSecurity(security, null);
    }

    public boolean addSecurity(String security, String name) {
        return addSecurity(Security.builder()
                .id(security)
                .name(name)
                .build());
    }

    public boolean addSecurity(Security security) {
        return handlePost(
                () -> securityRestController.post(security),
                "Не могу добавить ЦБ " + security + " в список");
    }

    public void addTransaction(SecurityTransaction securityTransaction) {
        boolean isAdded = addTransaction(securityTransaction.getTransaction());
        if (isAdded) {
            securityTransaction.getTransactionCashFlows().forEach(this::addTransactionCashFlow);
        }
    }

    public void addTransaction(DerivativeTransaction derivativeTransaction) {
        addSecurity(derivativeTransaction.getSecurity());
        boolean isAdded = addTransaction(derivativeTransaction.getTransaction());
        if (isAdded) {
            derivativeTransaction.getTransactionCashFlows().forEach(this::addTransactionCashFlow);
        }
    }

    public void addTransaction(ForeignExchangeTransaction fxTransaction) {
        addSecurity(fxTransaction.getSecurity());
        boolean isAdded = addTransaction(fxTransaction.getTransaction());
        if (isAdded) {
            fxTransaction.getTransactionCashFlows().forEach(this::addTransactionCashFlow);
        }
    }

    protected boolean addTransaction(Transaction transaction) {
        return handlePost(
                () -> transactionRestController.post(transaction),
                "Не могу добавить транзакцию " + transaction);
    }

    public void addTransactionCashFlow(TransactionCashFlow transactionCashFlow) {
        handlePost(
                () -> transactionCashFlowRestController.post(transactionCashFlow),
                "Не могу добавить информацию о передвижении средств " + transactionCashFlow);
    }

    public void addEventCashFlow(EventCashFlow eventCashFlow) {
        handlePost(
                () -> eventCashFlowRestController.post(eventCashFlow),
                "Не могу добавить информацию о движении денежных средств " + eventCashFlow);
    }

    public void addSecurityEventCashFlow(SecurityEventCashFlow securityEventCashFlow) {
        handlePost(
                () -> securityEventCashFlowRestController.post(securityEventCashFlow),
                "Не могу добавить информацию о движении денежных средств " + securityEventCashFlow);
    }

    // TODO replace by storing to new 'portfolio_cash' table
    @Deprecated
    public void addPortfolioCash(Collection<PortfolioCash> cash) {
        try {
            Map<Entry<String, Instant>, List<PortfolioCash>> groupedCash = cash.stream()
                    .collect(
                            groupingBy(
                                    v -> new SimpleEntry<>(v.getPortfolio(), v.getTimestamp()),
                                    toList()));
            groupedCash.entrySet()
                    .stream()
                    .map(e -> PortfolioProperty.builder()
                            .portfolio(e.getKey().getKey())
                            .timestamp(e.getKey().getValue())
                            .property(PortfolioPropertyType.CASH)
                            .value(PortfolioCash.serialize(e.getValue()))
                            .build())
                    .forEach(this::addPortfolioProperty);
        } catch (Exception e) {
            log.warn("Не могу добавить информацию о наличных средствах {}", cash, e);
        }
    }

    public void addPortfolioProperty(PortfolioProperty property) {
        handlePost(
                () -> portfolioPropertyRestController.post(property),
                "Не могу добавить информацию о свойствах портфеля " + property);
    }

    public void addSecurityQuote(SecurityQuote securityQuote) {
        handlePost(
                () -> securityQuoteRestController.post(securityQuote),
                "Не могу добавить информацию о котировке финансового инструмента " + securityQuote);
    }

    public void addForeignExchangeRate(ForeignExchangeRate exchangeRate) {
        handlePost(
                () -> foreignExchangeRateRestController.post(exchangeRate),
                "Не могу добавить информацию о курсе валюты " + exchangeRate);
    }

    /**
     * @return true if new row was added or it was already exists in DB, false - or error
     */
    private boolean handlePost(Supplier<ResponseEntity<?>> postAction, String error) {
        try {
            HttpStatus status = postAction.get().getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn(error);
                return false;
            }
        } catch (Exception e) {
            if (isUniqIndexViolationException(e)) {
                log.debug("Дублирование информации: {}", error);
                log.trace("Дублирование вызвано исключением", e);
                return true; // same as above status == HttpStatus.CONFLICT
            } else {
                log.warn(error, e);
                return false;
            }
        }
        return true;
    }
}

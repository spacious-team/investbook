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

package ru.investbook.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.pojo.Transaction;
import org.spacious_team.broker.pojo.TransactionCashFlow;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.investbook.api.EventCashFlowRestController;
import ru.investbook.api.ForeignExchangeRateRestController;
import ru.investbook.api.PortfolioCashRestController;
import ru.investbook.api.PortfolioPropertyRestController;
import ru.investbook.api.PortfolioRestController;
import ru.investbook.api.SecurityEventCashFlowRestController;
import ru.investbook.api.SecurityQuoteRestController;
import ru.investbook.api.SecurityRestController;
import ru.investbook.api.TransactionCashFlowRestController;
import ru.investbook.api.TransactionRestController;
import ru.investbook.service.moex.MoexDerivativeCodeService;

import java.util.Optional;
import java.util.function.Supplier;

import static org.spacious_team.broker.pojo.CashFlowType.DERIVATIVE_PROFIT;
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
    private final PortfolioCashRestController portfolioCashRestController;
    private final SecurityQuoteRestController securityQuoteRestController;
    private final ForeignExchangeRateRestController foreignExchangeRateRestController;
    private final MoexDerivativeCodeService moexDerivativeCodeService;

    public boolean addPortfolio(Portfolio portfolio) {
        return handlePost(
                () -> portfolioRestController.post(portfolio),
                "Не могу сохранить Портфель " + portfolio);
    }

    public void addSecurity(Security security) {
        Security _security = convertDerivativeSecurityId(security);
        handlePost(
                () -> securityRestController.post(_security),
                "Не могу добавить ЦБ " + security + " в список");
    }

    private Security convertDerivativeSecurityId(Security security) {
        return security.getType() == SecurityType.DERIVATIVE ?
                security.toBuilder()
                        .ticker(moexDerivativeCodeService.convertDerivativeCode(security.getTicker()))
                        .build() :
                security;
    }

    public void addTransaction(AbstractTransaction transaction) {
        boolean isAdded = addTransaction(transaction.getTransaction());
        if (isAdded) {
            Optional.ofNullable(transaction.getId())
                    .or(() -> getSavedTransactionId(transaction))
                    .ifPresentOrElse(
                            transactionId -> addCashTransactionFlows(transaction, transactionId),
                            () -> log.warn("Не могу добавить транзакцию в БД, " +
                                    "не задан внутренний идентификатор записи: {}", transaction));
        }
    }

    private Optional<Integer> getSavedTransactionId(AbstractTransaction transaction) {
        return Optional.of(transactionRestController.get(transaction.getPortfolio(), transaction.getTradeId()))
                .filter(result -> result.size() == 1)
                .map(result -> result.get(0))
                .map(Transaction::getId);
    }

    private void addCashTransactionFlows(AbstractTransaction transaction, int transactionId) {
        transaction.toBuilder()
                .id(transactionId)
                .build()
                .getTransactionCashFlows()
                .forEach(this::addTransactionCashFlow);
    }

    private boolean addTransaction(Transaction transaction) {
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

    public void addSecurityEventCashFlow(SecurityEventCashFlow cf) {
        SecurityEventCashFlow securityEventCashFlow = (cf.getCount() == null && cf.getEventType() == DERIVATIVE_PROFIT) ?
                cf.toBuilder().count(0).build() : // count is optional for derivatives
                cf;
        handlePost(
                () -> securityEventCashFlowRestController.post(securityEventCashFlow),
                "Не могу добавить информацию о движении денежных средств " + securityEventCashFlow);
    }

    public void addPortfolioCash(PortfolioCash cash) {
        handlePost(
                () -> portfolioCashRestController.post(cash),
                "Не могу добавить информацию об остатках денежных средств портфеля " + cash);
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

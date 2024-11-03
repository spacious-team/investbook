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

package ru.investbook.parser;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityDescription;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.pojo.Transaction;
import org.spacious_team.broker.pojo.TransactionCashFlow;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.springframework.stereotype.Component;
import ru.investbook.api.CreateResult;
import ru.investbook.api.EventCashFlowRestController;
import ru.investbook.api.ForeignExchangeRateRestController;
import ru.investbook.api.PortfolioCashRestController;
import ru.investbook.api.PortfolioPropertyRestController;
import ru.investbook.api.PortfolioRestController;
import ru.investbook.api.SecurityDescriptionRestController;
import ru.investbook.api.SecurityEventCashFlowRestController;
import ru.investbook.api.SecurityQuoteRestController;
import ru.investbook.api.SecurityRestController;
import ru.investbook.api.TransactionCashFlowRestController;
import ru.investbook.api.TransactionRestController;
import ru.investbook.service.moex.MoexDerivativeCodeService;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.spacious_team.broker.pojo.CashFlowType.DERIVATIVE_PROFIT;
import static ru.investbook.repository.RepositoryHelper.isUniqIndexViolationException;

@Component
@Slf4j
@RequiredArgsConstructor
public class InvestbookApiClient {
    private final PortfolioRestController portfolioRestController;
    private final SecurityRestController securityRestController;
    private final SecurityDescriptionRestController securityDescriptionRestController;
    private final SecurityEventCashFlowRestController securityEventCashFlowRestController;
    private final EventCashFlowRestController eventCashFlowRestController;
    private final TransactionRestController transactionRestController;
    private final TransactionCashFlowRestController transactionCashFlowRestController;
    private final PortfolioPropertyRestController portfolioPropertyRestController;
    private final PortfolioCashRestController portfolioCashRestController;
    private final SecurityQuoteRestController securityQuoteRestController;
    private final ForeignExchangeRateRestController foreignExchangeRateRestController;
    private final MoexDerivativeCodeService moexDerivativeCodeService;
    private final ValidatorService validator;

    public boolean addPortfolio(Portfolio portfolio) {
        return saveWithoutUpdate(
                portfolio,
                portfolioRestController::createIfAbsent,
                "Не могу сохранить Портфель");
    }

    public void addSecurity(Security security) {
        security = convertDerivativeSecurityId(security);
        saveWithoutUpdate(
                security,
                securityRestController::createIfAbsent,
                "Не могу добавить ЦБ ");
    }

    private Security convertDerivativeSecurityId(Security security) {
        return security.getType() == SecurityType.DERIVATIVE ?
                security.toBuilder()
                        .ticker(moexDerivativeCodeService.convertDerivativeCode(security.getTicker()))
                        .build() :
                security;
    }

    public void addSecurityDescription(SecurityDescription securityDescription) {
        saveWithoutUpdate(
                securityDescription,
                securityDescriptionRestController::post,
                "Не могу добавить метаинформацию о ЦБ ");
    }

    public void addTransaction(AbstractTransaction transaction) {
        saveWithoutUpdateAndGet(
                transaction.getTransaction(),
                transactionRestController::createIfAbsentAndGet,
                "Не могу добавить транзакцию")
                .map(Transaction::getId)
                .ifPresentOrElse(
                        transactionId -> addCashTransactionFlows(transaction, transactionId),
                        () -> log.warn("Не могу добавить транзакцию в БД, " +
                                "не задан внутренний идентификатор записи: {}", transaction));
    }

    private void addCashTransactionFlows(AbstractTransaction transaction, int transactionId) {
        transaction.toBuilder()
                .id(transactionId)
                .build()
                .getTransactionCashFlows()
                .forEach(this::addTransactionCashFlow);
    }

    public void addTransaction(Transaction transaction) {
        saveWithoutUpdate(
                transaction,
                transactionRestController::createIfAbsent,
                "Не могу добавить транзакцию");
    }

    public void addTransactionCashFlow(TransactionCashFlow transactionCashFlow) {
        saveWithoutUpdate(
                transactionCashFlow,
                transactionCashFlowRestController::createIfAbsent,
                "Не могу добавить информацию о передвижении средств");
    }

    public void addEventCashFlow(EventCashFlow eventCashFlow) {
        saveWithoutUpdate(
                eventCashFlow,
                eventCashFlowRestController::createIfAbsent,
                "Не могу добавить информацию о движении денежных средств");
    }

    public void addSecurityEventCashFlow(SecurityEventCashFlow cf) {
        if (cf.getCount() == null && cf.getEventType() == DERIVATIVE_PROFIT) {
            cf = cf.toBuilder().count(0).build(); // count is optional for derivatives
        }
        saveWithoutUpdate(
                cf,
                securityEventCashFlowRestController::createIfAbsent,
                "Не могу добавить информацию о движении денежных средств");
    }

    public void addPortfolioCash(PortfolioCash cash) {
        saveWithoutUpdate(
                cash,
                portfolioCashRestController::createIfAbsent,
                "Не могу добавить информацию об остатках денежных средств портфеля");
    }

    public void addPortfolioProperty(PortfolioProperty property) {
        saveWithoutUpdate(
                property,
                portfolioPropertyRestController::createIfAbsent,
                "Не могу добавить информацию о свойствах портфеля");
    }

    public void addSecurityQuote(SecurityQuote securityQuote) {
        saveWithoutUpdate(
                securityQuote,
                securityQuoteRestController::createIfAbsent,
                "Не могу добавить информацию о котировке финансового инструмента");
    }

    public void addForeignExchangeRate(ForeignExchangeRate exchangeRate) {
        saveWithoutUpdate(
                exchangeRate,
                foreignExchangeRateRestController::createIfAbsent,
                "Не могу добавить информацию о курсе валюты");
    }

    /**
     * @return true - if object was created, or it was already exists in DB,
     * false - if object not exists and create error was occurred
     */
    private <T> boolean saveWithoutUpdate(T object, Consumer<T> persistFunction, String errorMsg) {
        try {
            validator.validate(object);
            persistFunction.accept(object);
            return true;
        } catch (ConstraintViolationException e) {  // jakarta.validation, not SQL constraint
            log.warn("{}, {}: {}", errorMsg, e.getMessage(), object);
            return false;
        } catch (Exception e) {
            if (isUniqIndexViolationException(e)) {
                log.debug("Дублирование информации: {} {}", errorMsg, object);
                log.trace("Дублирование вызвано исключением", e);
                return true;  // object already exists
            } else {
                log.warn("{} {}", errorMsg, object, e);
                return false;
            }
        }
    }

    /**
     * @return true - if object was created, or it was already exists in DB,
     * false - if object not exists and create error was occurred
     */
    private <T> Optional<T> saveWithoutUpdateAndGet(T object, Function<T, CreateResult<T>> persistFunction,
                                                    @SuppressWarnings("SameParameterValue") String errorMsg) {
        try {
            validator.validate(object);
            CreateResult<T> result = persistFunction.apply(object);
            return Optional.of(result.object());
        } catch (Exception e) {
            // should not be thrown for duplicate
            log.warn("{}, {}: {}", errorMsg, e.getMessage(), object);
            return Optional.empty();
        }
    }
}

/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.portfolio.portfolio.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.controller.*;
import ru.portfolio.portfolio.pojo.*;

import java.util.function.Supplier;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReportTableStorage {
    private final PortfolioRestController portfolioRestController;
    private final SecurityRestController securityRestController;
    private final SecurityEventCashFlowRestController securityEventCashFlowRestController;
    private final EventCashFlowRestController eventCashFlowRestController;
    private final TransactionRestController transactionRestController;
    private final TransactionCashFlowRestController transactionCashFlowRestController;
    private final PortfolioPropertyRestController portfolioPropertyRestController;
    private final ObjectMapper objectMapper;

    public boolean addPortfolio(Portfolio portfolio) {
        return handlePost(
                () -> portfolioRestController.post(portfolio),
                "Не могу сохранить Портфель " + portfolio);
    }

    public boolean addSecurity(String isin) {
        return addSecurity(isin, null);
    }

    public boolean addSecurity(String isin, String name) {
        return addSecurity(Security.builder()
                .isin(isin)
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
        addSecurity(derivativeTransaction.getContract());
        boolean isAdded = addTransaction(derivativeTransaction.getTransaction());
        if (isAdded) {
            derivativeTransaction.getTransactionCashFlows().forEach(this::addTransactionCashFlow);
        }
    }

    public void addTransaction(ForeignExchangeTransaction fxTransaction) {
        addSecurity(fxTransaction.getInstrument());
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

    public void addPortfolioProperty(PortfolioProperty property) {
        handlePost(
                () -> portfolioPropertyRestController.post(property),
                "Не могу добавить информацию о свойствах портфеля " + property);
    }

    public void addCashInfo(ReportTable<PortfolioCash> cashTable) {
        try {
            if (!cashTable.getData().isEmpty()) {
                addPortfolioProperty(PortfolioProperty.builder()
                        .portfolio(cashTable.getReport().getPortfolio())
                        .property(PortfolioPropertyType.CASH)
                        .value(objectMapper.writeValueAsString(cashTable.getData()))
                        .timestamp(cashTable.getReport().getReportDate())
                        .build());
            }
        } catch (JsonProcessingException e) {
            log.warn("Не могу добавить информацию о наличных средствах {}", cashTable.getData(), e);
        }
    }

    private boolean handlePost(Supplier<ResponseEntity<?>> postAction, String error) {
        try {
            HttpStatus status = postAction.get().getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn(error);
                return false;
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: {}", error, e);
            } else {
                log.warn(error);
            }
            return false;
        }
        return true;
    }
}

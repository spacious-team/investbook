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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.controller.*;
import ru.portfolio.portfolio.pojo.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReportTableSaver {
    private final PortfolioRestController portfolioRestController;
    private final SecurityRestController securityRestController;
    private final SecurityEventCashFlowRestController securityEventCashFlowRestController;
    private final EventCashFlowRestController eventCashFlowRestController;
    private final TransactionRestController transactionRestController;
    private final TransactionCashFlowRestController transactionCashFlowRestController;
    private final PortfolioPropertyRestController portfolioPropertyRestController;

    public boolean addSecurity(String isin) {
        return addSecurity(isin, null);
    }

    public boolean addSecurity(String isin, String name) {
        return addSecurity(Security.builder()
                .isin(isin)
                .name(name));
    }

    public boolean addPortfolio(Portfolio.PortfolioBuilder portfolio) {
        try {
            HttpStatus status = portfolioRestController.post(portfolio.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу сохранить Портфель {}", portfolio);
                return false;
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу сохранить Портфель {}", portfolio, e);
            } else {
                log.warn("Не могу сохранить Портфель {}", portfolio, e);
            }
            return false;
        }
        return true;
    }

    public boolean addSecurity(Security.SecurityBuilder security) {
        try {
            HttpStatus status = securityRestController.post(security.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить ЦБ {} в список", security);
                return false;
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу добавить ЦБ {} в список", security, e);
            } else {
                log.warn("Не могу добавить ЦБ {} в список", security, e);
            }
            return false;
        }
        return true;
    }

    public boolean addTransaction(Transaction.TransactionBuilder transaction) {
        try {
            HttpStatus status = transactionRestController.post(transaction.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить транзакцию {}", transaction);
                return false;
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу добавить транзакцию {}", transaction, e);
            } else {
                log.warn("Не могу добавить транзакцию {}", transaction, e);
            }
            return false;
        }
        return true;
    }

    public void addTransactionCashFlow(TransactionCashFlow.TransactionCashFlowBuilder transactionCashFlow) {
        try {
            HttpStatus status = transactionCashFlowRestController.post(transactionCashFlow.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить информацию о передвижении средств {}", transactionCashFlow);
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу добавить информацию о передвижении средств {}", transactionCashFlow, e);
            } else {
                log.warn("Не могу добавить информацию о передвижении средств {}", transactionCashFlow, e);
            }
        }
    }

    public void addEventCashFlow(EventCashFlow.EventCashFlowBuilder eventCashFlow) {
        try {
            HttpStatus status = eventCashFlowRestController.post(eventCashFlow.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить информацию о движении денежных средств {}", eventCashFlow);
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу добавить информацию о движении денежных средств {}", eventCashFlow, e);
            } else {
                log.warn("Не могу добавить информацию о движении денежных средств {}", eventCashFlow, e);
            }
        }
    }

    public void addSecurityEventCashFlow(SecurityEventCashFlow.SecurityEventCashFlowBuilder securityEventCashFlow) {
        try {
            HttpStatus status = securityEventCashFlowRestController.post(securityEventCashFlow.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить информацию о движении денежных средств {}", securityEventCashFlow);
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу добавить информацию о движении денежных средств {}", securityEventCashFlow, e);
            } else {
                log.warn("Не могу добавить информацию о движении денежных средств {}", securityEventCashFlow, e);
            }
        }
    }

    public void addPortfolioProperty(PortfolioProperty.PortfolioPropertyBuilder property) {
        try {
            HttpStatus status = portfolioPropertyRestController.post(property.build()).getStatusCode();
            if (!status.is2xxSuccessful() && status != HttpStatus.CONFLICT) {
                log.warn("Не могу добавить информацию о свойствах портфеля {}", property);
            }
        } catch (Exception e) {
            if (NestedExceptionUtils.getMostSpecificCause(e).getMessage().toLowerCase().contains("duplicate")) {
                log.debug("Дублирование информации: не могу добавить информацию о свойствах портфеля {}", property, e);
            } else {
                log.warn("Не могу добавить информацию о свойствах портфеля {}", property, e);
            }
        }
    }
}

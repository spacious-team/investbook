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

package ru.portfolio.portfolio.view.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.SecurityEventCashFlowConverter;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.Portfolio;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;
import ru.portfolio.portfolio.repository.SecurityEventCashFlowRepository;
import ru.portfolio.portfolio.repository.SecurityRepository;
import ru.portfolio.portfolio.view.Table;
import ru.portfolio.portfolio.view.TableFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.portfolio.portfolio.pojo.CashFlowType.*;
import static ru.portfolio.portfolio.view.excel.PortfolioPaymentTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioPaymentTableFactory implements TableFactory {
    private static final CashFlowType[] PAY_TYPES = new CashFlowType[]{AMORTIZATION, REDEMPTION, COUPON, DIVIDEND, TAX};
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;
    private final SecurityRepository securityRepository;
    private final ForeignExchangeRateTableFactory foreignExchangeRateTableFactory;

    @Override
    public Table create(Portfolio portfolio) {
        List<SecurityEventCashFlow> cashFlows = getCashFlows(portfolio);
        return getTable(cashFlows);
    }

    private ArrayList<SecurityEventCashFlow> getCashFlows(Portfolio portfolio) {
        return securityEventCashFlowRepository
                .findByPortfolioIdAndCashFlowTypeIdInOrderByTimestampDesc(
                        portfolio.getId(),
                        Stream.of(PAY_TYPES)
                                .map(CashFlowType::getId)
                                .collect(Collectors.toList()))
                .stream()
                .map(securityEventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Table getTable(List<SecurityEventCashFlow> cashFlows) {
        Table table = new Table();
        for (SecurityEventCashFlow cash : cashFlows) {
            Table.Record record = new Table.Record();
            record.put(DATE, cash.getTimestamp());
            record.put(COUNT, cash.getCount());
            record.put(PortfolioPaymentTableHeader.CASH, cash.getValue());
            record.put(CURRENCY, cash.getCurrency());
            record.put(CASH_RUB, foreignExchangeRateTableFactory.cashConvertToRubExcelFormula(cash.getCurrency(),
                    PortfolioPaymentTableHeader.CASH, EXCHANGE_RATE));
            record.put(DESCRIPTION,  getDescription(cash));
            table.add(record);
        }
        if (!cashFlows.isEmpty()) {
            foreignExchangeRateTableFactory.appendExchangeRates(table, CURRENCY_NAME, EXCHANGE_RATE);
        }
        return table;
    }

    private String getDescription(SecurityEventCashFlow cash) {
        String paymentType = "Выплата";
        switch (cash.getEventType()) {
            case DIVIDEND:
                paymentType = "Дивиденды";
                break;
            case COUPON:
                paymentType = "Купоны";
                break;
            case REDEMPTION:
                paymentType = "Погашение облигации";
                break;
            case AMORTIZATION:
                paymentType = "Амортизация облигаци";
                break;
            case TAX:
                paymentType = "Удержание налога c выплаты";
                break;
        }
        String security = securityRepository.findByIsin(cash.getIsin())
                .map(SecurityEntity::getName)
                .map(name -> name + " (" + cash.getIsin() + ")")
                .orElse(cash.getIsin());
        return paymentType + " по " + security;
    }
}

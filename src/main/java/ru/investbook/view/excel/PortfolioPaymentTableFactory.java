/*
 * InvestBook
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

package ru.investbook.view.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.investbook.converter.SecurityEventCashFlowConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.pojo.CashFlowType;
import ru.investbook.pojo.Portfolio;
import ru.investbook.pojo.SecurityEventCashFlow;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.view.Table;
import ru.investbook.view.TableFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.investbook.pojo.CashFlowType.*;
import static ru.investbook.view.excel.PortfolioPaymentTableHeader.*;

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
        if (!cashFlows.isEmpty()) {
            table.add(new Table.Record());
            Table.Record monthTotalRecord = new Table.Record();
            table.add(monthTotalRecord);
            Month month = null;
            int sumRowCount = 0;
            for (SecurityEventCashFlow cash : cashFlows) {
                Instant timestamp = cash.getTimestamp();
                Month currentMonth = LocalDate.ofInstant(timestamp, ZoneId.systemDefault()).getMonth();
                if (month == null) {
                    month = currentMonth;
                } else if (currentMonth != month) {
                    calcTotalRecord(monthTotalRecord, month, sumRowCount);
                    table.add(new Table.Record());
                    monthTotalRecord = new Table.Record();
                    table.add(monthTotalRecord);
                    month = currentMonth;
                    sumRowCount = 0;
                }
                Table.Record record = new Table.Record();
                record.put(DATE, timestamp);
                record.put(COUNT, cash.getCount());
                record.put(PortfolioPaymentTableHeader.CASH, cash.getValue());
                record.put(CURRENCY, cash.getCurrency());
                record.put(CASH_RUB, foreignExchangeRateTableFactory.cashConvertToRubExcelFormula(cash.getCurrency(),
                        PortfolioPaymentTableHeader.CASH, EXCHANGE_RATE));
                record.put(DESCRIPTION, getDescription(cash));
                table.add(record);
                sumRowCount++;
            }
            calcTotalRecord(monthTotalRecord, month, sumRowCount);
            if (!cashFlows.isEmpty()) {
                foreignExchangeRateTableFactory.appendExchangeRates(table, CURRENCY_NAME, EXCHANGE_RATE);
            }
        }
        return table;
    }

    private void calcTotalRecord(Table.Record monthTotalRecord, Month month, int sumRowCount) {
        if (sumRowCount != 0) {
            monthTotalRecord.put(DATE, StringUtils.capitalize(month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())));
            monthTotalRecord.put(CASH_RUB, "=SUM(OFFSET(" + CASH_RUB.getCellAddr() + ",1,0," + sumRowCount + ",1))");
        }
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

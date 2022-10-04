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
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.TransactionRepository;

import static java.util.Optional.ofNullable;
import static ru.investbook.report.excel.SecuritiesDepositAndWithdrawalExcelTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecuritiesDepositAndWithdrawalExcelTableFactory implements TableFactory {
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    @Override
    public Table create(Portfolio portfolio) {
        Table table = new Table();
        ViewFilter viewFilter = ViewFilter.get();
        for (TransactionEntity transactionEntity :
                transactionRepository.findByPortfolioAndTimestampBetweenDepositAndWithdrawalTransactions(
                        portfolio, viewFilter.getFromDate(), viewFilter.getToDate())) {
            Table.Record record = new Table.Record();
            table.add(record);
            record.put(DATE, transactionEntity.getTimestamp());
            record.put(COUNT, transactionEntity.getCount());
            SecurityEntity securityEntity = transactionEntity.getSecurity();
            record.put(SECURITY, ofNullable(securityEntity.getName())
                    .or(() -> ofNullable(securityEntity.getTicker()))
                    .or(() -> ofNullable(securityEntity.getIsin()))
                    .orElse("<неизвестно>"));
        }
        return table;
    }
}

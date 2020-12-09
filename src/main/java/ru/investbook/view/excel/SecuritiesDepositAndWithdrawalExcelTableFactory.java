/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.view.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.stereotype.Component;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.view.Table;
import ru.investbook.view.TableFactory;
import ru.investbook.view.ViewFilter;

import java.util.Optional;

import static ru.investbook.view.excel.SecuritiesDepositAndWithdrawalExcelTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecuritiesDepositAndWithdrawalExcelTableFactory implements TableFactory {
    private final TransactionRepository transactionRepository;

    @Override
    public Table create(Portfolio portfolio) {
        Table table = new Table();
        for (TransactionEntity transactionEntity :
                transactionRepository.findByPkPortfolioAndTimestampBetweenDepositAndWithdrawalTransactions(
                        portfolio, ViewFilter.get().getFromDate(), ViewFilter.get().getToDate())) {
            Table.Record record = new Table.Record();
            table.add(record);
            record.put(DATE, transactionEntity.getTimestamp());
            record.put(COUNT, transactionEntity.getCount());
            SecurityEntity securityEntity = transactionEntity.getSecurity();
            record.put(SECURITY, Optional.ofNullable(securityEntity.getName())
                    .orElse(securityEntity.getId()));
        }
        return table;
    }
}

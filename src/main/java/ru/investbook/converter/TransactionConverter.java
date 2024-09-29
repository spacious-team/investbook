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

package ru.investbook.converter;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.stereotype.Component;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.repository.SecurityRepository;

@Component
@RequiredArgsConstructor
public class TransactionConverter implements EntityConverter<TransactionEntity, Transaction> {
    private final SecurityRepository securityRepository;

    @Override
    public TransactionEntity toEntity(Transaction transaction) {
        SecurityEntity securityEntity = securityRepository.getReferenceById(transaction.getSecurity());

        TransactionEntity entity = new TransactionEntity();
        entity.setId(transaction.getId());
        entity.setTradeId(transaction.getTradeId());
        entity.setPortfolio(transaction.getPortfolio());
        entity.setSecurity(securityEntity);
        entity.setTimestamp(transaction.getTimestamp());
        entity.setCount(transaction.getCount());
        return entity;
    }

    @Override
    public Transaction fromEntity(TransactionEntity entity) {
        return Transaction.builder()
                .id(entity.getId())
                .tradeId(entity.getTradeId())
                .portfolio(entity.getPortfolio())
                .security(entity.getSecurity().getId())
                .timestamp(entity.getTimestamp())
                .count(entity.getCount())
                .build();
    }
}

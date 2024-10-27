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
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.TransactionCashFlow;
import org.springframework.stereotype.Component;
import ru.investbook.entity.CashFlowTypeEntity;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.repository.CashFlowTypeRepository;

@Component
@RequiredArgsConstructor
public class TransactionCashFlowConverter implements EntityConverter<TransactionCashFlowEntity, TransactionCashFlow> {
    private final CashFlowTypeRepository cashFlowTypeRepository;

    @SuppressWarnings({"nullness", "DataFlowIssue"})
    @Override
    public TransactionCashFlowEntity toEntity(TransactionCashFlow cash) {
        CashFlowTypeEntity cashFlowTypeEntity = cashFlowTypeRepository.getReferenceById(cash.getEventType().getId());

        TransactionCashFlowEntity entity = new TransactionCashFlowEntity();
        entity.setId(cash.getId());
        entity.setTransactionId(cash.getTransactionId());
        entity.setCashFlowType(cashFlowTypeEntity);
        entity.setValue(cash.getValue());
        //noinspection ConstantValue
        if (cash.getCurrency() != null) entity.setCurrency(cash.getCurrency());
        return entity;
    }

    @Override
    public TransactionCashFlow fromEntity(TransactionCashFlowEntity entity) {
        return TransactionCashFlow.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .eventType(CashFlowType.valueOf(entity.getCashFlowType().getId()))
                .value(entity.getValue())
                .currency(entity.getCurrency())
                .build();
    }
}

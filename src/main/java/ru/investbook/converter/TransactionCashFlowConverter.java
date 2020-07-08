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

package ru.investbook.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionCashFlowEntityPK;
import ru.investbook.entity.TransactionEntityPK;
import ru.investbook.pojo.CashFlowType;
import ru.investbook.pojo.TransactionCashFlow;
import ru.investbook.repository.CashFlowTypeRepository;
import ru.investbook.repository.TransactionRepository;

@Component
@RequiredArgsConstructor
public class TransactionCashFlowConverter implements EntityConverter<TransactionCashFlowEntity, TransactionCashFlow> {
    private final TransactionRepository transactionRepository;
    private final CashFlowTypeRepository cashFlowTypeRepository;

    @Override
    public TransactionCashFlowEntity toEntity(TransactionCashFlow cash) {
        TransactionEntityPK transactionPk = new TransactionEntityPK();
        transactionPk.setId(cash.getTransactionId());
        transactionPk.setPortfolio(cash.getPortfolio());
        if (!transactionRepository.existsById(transactionPk))
            throw new IllegalArgumentException("Транзакция с номером не найдена: " + cash.getTransactionId());
        if (!cashFlowTypeRepository.existsById(cash.getEventType().getId()))
            throw new IllegalArgumentException("В справочнике не найдено событие с типом: " + cash.getEventType().getId());

        TransactionCashFlowEntityPK pk = new TransactionCashFlowEntityPK();
        pk.setTransactionId(cash.getTransactionId());
        pk.setPortfolio(cash.getPortfolio());
        pk.setType(cash.getEventType().getId());
        TransactionCashFlowEntity entity = new TransactionCashFlowEntity();
        entity.setPk(pk);
        entity.setValue(cash.getValue());
        if (cash.getCurrency() != null) entity.setCurrency(cash.getCurrency());
        return entity;
    }

    @Override
    public TransactionCashFlow fromEntity(TransactionCashFlowEntity entity) {
        return TransactionCashFlow.builder()
                .transactionId(entity.getPk().getTransactionId())
                .portfolio(entity.getPk().getPortfolio())
                .eventType(CashFlowType.valueOf(entity.getPk().getType()))
                .value(entity.getValue())
                .currency(entity.getCurrency())
                .build();
    }
}

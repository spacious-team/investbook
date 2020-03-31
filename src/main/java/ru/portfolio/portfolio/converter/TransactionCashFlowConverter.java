package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntity;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntityPK;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.TransactionCashFlow;
import ru.portfolio.portfolio.repository.CashFlowTypeRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;

@Component
@RequiredArgsConstructor
public class TransactionCashFlowConverter implements EntityConverter<TransactionCashFlowEntity, TransactionCashFlow> {
    private final TransactionRepository transactionRepository;
    private final CashFlowTypeRepository cashFlowTypeRepository;

    @Override
    public TransactionCashFlowEntity toEntity(TransactionCashFlow cash) {
        if (!transactionRepository.existsById(cash.getTransactionId()))
            throw new IllegalArgumentException("Транзакция с номером не найдена: " + cash.getTransactionId());
        if (!cashFlowTypeRepository.existsById(cash.getEventType().getType()))
            throw new IllegalArgumentException("В справочнике не найдено событие с типом: " + cash.getEventType().getType());

        TransactionCashFlowEntityPK pk = new TransactionCashFlowEntityPK();
        pk.setTransactionId(cash.getTransactionId());
        pk.setType(cash.getEventType().getType());
        TransactionCashFlowEntity entity = new TransactionCashFlowEntity();
        entity.setTransactionCashFlowId(pk);
        entity.setValue(cash.getValue());
        if (cash.getCurrency() != null) entity.setCurrency(cash.getCurrency());
        return entity;
    }

    @Override
    public TransactionCashFlow fromEntity(TransactionCashFlowEntity entity) {
        return TransactionCashFlow.builder()
                .transactionId(entity.getTransactionCashFlowId().getTransactionId())
                .eventType(CashFlowType.valueOf(entity.getTransactionCashFlowId().getType()))
                .value(entity.getValue())
                .currency(entity.getCurrency())
                .build();
    }
}

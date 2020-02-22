package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.entity.TransactionEntity;
import ru.portfolio.portfolio.pojo.Transaction;
import ru.portfolio.portfolio.repository.SecurityRepository;

@Component
@RequiredArgsConstructor
public class TransactionEntityConverter implements EntityConverter<TransactionEntity, Transaction> {
    private final SecurityRepository securityRepository;

    @Override
    public TransactionEntity toEntity(Transaction transaction) {
        SecurityEntity securityEntity = securityRepository.findByIsin(transaction.getIsin())
                .orElseThrow(() -> new IllegalArgumentException("Ценная бумага с заданным ISIN не найдена: " + transaction.getIsin()));

        TransactionEntity entity = new TransactionEntity();
        entity.setId(transaction.getId());
        entity.setSecurity(securityEntity);
        entity.setTimestamp(transaction.getTimestamp());
        entity.setCount(transaction.getCount());
        return entity;
    }
}

package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.entity.TransactionEntity;
import ru.portfolio.portfolio.pojo.Transaction;
import ru.portfolio.portfolio.repository.PortfolioRepository;
import ru.portfolio.portfolio.repository.SecurityRepository;

@Component
@RequiredArgsConstructor
public class TransactionEntityConverter implements EntityConverter<TransactionEntity, Transaction> {
    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;

    @Override
    public TransactionEntity toEntity(Transaction transaction) {
        PortfolioEntity portfolioEntity = portfolioRepository.findById(transaction.getPortfolio())
                .orElseThrow(() -> new IllegalArgumentException("В справочнике не найден брокерский счет: " + transaction.getPortfolio()));
        SecurityEntity securityEntity = securityRepository.findByIsin(transaction.getIsin())
                .orElseThrow(() -> new IllegalArgumentException("Ценная бумага с заданным ISIN не найдена: " + transaction.getIsin()));

        TransactionEntity entity = new TransactionEntity();
        entity.setId(transaction.getId());
        entity.setPortfolio(portfolioEntity);
        entity.setSecurity(securityEntity);
        entity.setTimestamp(transaction.getTimestamp());
        entity.setCount(transaction.getCount());
        return entity;
    }

    @Override
    public Transaction fromEntity(TransactionEntity entity) {
        return Transaction.builder()
                .id(entity.getId())
                .portfolio(entity.getPortfolio().getPortfolio())
                .isin(entity.getSecurity().getIsin())
                .timestamp(entity.getTimestamp())
                .count(entity.getCount())
                .build();
    }
}

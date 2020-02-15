package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.IssuerEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.repository.IssuerRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityEntityConverter implements EntityConverter<SecurityEntity, Security> {

    private final IssuerRepository issuerRepository;

    @Override
    public SecurityEntity toEntity(Security security) {
        Optional<IssuerEntity> optionalIssuerEntity = issuerRepository.findByInn(security.getInn());
        IssuerEntity issuerEntity = optionalIssuerEntity
                .orElseGet(() -> {throw new IllegalArgumentException("Эмитетнт с ИНН не найден: " + security.getInn());});

        SecurityEntity entity = new SecurityEntity();
        entity.setTicker(security.getTicker());
        entity.setName(security.getName());
        entity.setIsin(security.getIsin());
        entity.setIssuer(issuerEntity);
        return entity;
    }
}

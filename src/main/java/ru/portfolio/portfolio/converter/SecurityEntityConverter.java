package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.IssuerEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.repository.IssuerRepository;

@Component
@RequiredArgsConstructor
public class SecurityEntityConverter implements EntityConverter<SecurityEntity, Security> {

    private final IssuerRepository issuerRepository;

    @Override
    public SecurityEntity toEntity(Security security) {
        IssuerEntity issuerEntity = issuerRepository.findByInn(security.getInn())
                .orElseGet(() -> {throw new IllegalArgumentException("Эмитетнт с ИНН не найден: " + security.getInn());});

        SecurityEntity entity = new SecurityEntity();
        entity.setTicker(security.getTicker());
        entity.setName(security.getName());
        entity.setIsin(security.getIsin());
        entity.setIssuer(issuerEntity);
        return entity;
    }
}

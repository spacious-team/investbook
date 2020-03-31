package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.IssuerEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.repository.IssuerRepository;

@Component
@RequiredArgsConstructor
public class SecurityConverter implements EntityConverter<SecurityEntity, Security> {

    private final IssuerRepository issuerRepository;

    @Override
    public SecurityEntity toEntity(Security security) {
        IssuerEntity issuerEntity = null;
        if (security.getInn() != null) {
            issuerEntity = issuerRepository.findByInn(security.getInn())
                    .orElseThrow(() -> new IllegalArgumentException("Эмитетнт с ИНН не найден: " + security.getInn()));
        }

        SecurityEntity entity = new SecurityEntity();
        entity.setTicker(security.getTicker());
        entity.setName(security.getName());
        entity.setIsin(security.getIsin());
        entity.setIssuer(issuerEntity);
        return entity;
    }

    @Override
    public Security fromEntity(SecurityEntity entity) {
        return Security.builder()
                .isin(entity.getIsin())
                .ticker(entity.getTicker())
                .name(entity.getName())
                .inn((entity.getIssuer() != null) ? entity.getIssuer().getInn() : null)
                .build();
    }
}

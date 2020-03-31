package ru.portfolio.portfolio.converter;

import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.IssuerEntity;
import ru.portfolio.portfolio.pojo.Issuer;

@Component
public class IssuerConverter implements EntityConverter<IssuerEntity, Issuer>  {

    @Override
    public IssuerEntity toEntity(Issuer issuer) {
        IssuerEntity entity = new IssuerEntity();
        entity.setInn(issuer.getInn());
        entity.setName(issuer.getName());
        return entity;
    }

    @Override
    public Issuer fromEntity(IssuerEntity entity) {
        return Issuer.builder()
                .inn(entity.getInn())
                .name(entity.getName())
                .build();
    }
}

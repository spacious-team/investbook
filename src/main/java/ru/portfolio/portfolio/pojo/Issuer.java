package ru.portfolio.portfolio.pojo;

import lombok.Builder;
import lombok.Getter;
import ru.portfolio.portfolio.dao.IssuerEntity;

import javax.validation.constraints.NotNull;

@Getter
@Builder
public class Issuer {
    @NotNull
    private final Long inn;
    @NotNull
    private final String name;

    public static Issuer of(IssuerEntity entity) {
        return Issuer.builder()
                .inn(entity.getInn())
                .name(entity.getName())
                .build();
    }

    public IssuerEntity toEntity() {
        IssuerEntity entity = new IssuerEntity();
        entity.setInn(this.inn);
        entity.setName(this.name);
        return entity;
    }
}

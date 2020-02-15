package ru.portfolio.portfolio.converter;

public interface EntityConverter<Entity, Pojo> {

    Entity toEntity(Pojo pojo);

    default Pojo fromEntity(Entity entity) {
        throw new UnsupportedOperationException();
    }
}

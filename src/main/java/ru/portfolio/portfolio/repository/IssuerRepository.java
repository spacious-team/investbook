package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.IssuerEntity;

import java.util.Optional;

public interface IssuerRepository extends JpaRepository<IssuerEntity, Long> {
    Optional<IssuerEntity> findByInn(Long integer);
}

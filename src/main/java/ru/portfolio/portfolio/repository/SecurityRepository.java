package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.SecurityEntity;

import java.util.Optional;

public interface SecurityRepository extends JpaRepository<SecurityEntity, String> {
    Optional<SecurityEntity> findByIsin(String isin);
}

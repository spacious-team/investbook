package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.PortfolioEntity;

import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<PortfolioEntity, String> {
    Optional<PortfolioEntity> findByPortfolio(String portfolio);
}

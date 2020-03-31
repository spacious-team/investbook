package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.PortfolioEntity;

public interface PortfolioRepository extends JpaRepository<PortfolioEntity, String> {
}

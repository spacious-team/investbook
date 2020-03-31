package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.PortfolioPropertyEntity;

import java.util.Optional;

public interface PortfolioPropertyRepository extends JpaRepository<PortfolioPropertyEntity, Integer> {

    Optional<PortfolioPropertyEntity> findFirstByPortfolioPortfolioAndPropertyOrderByTimestampDesc(String portfolio, String property);
}

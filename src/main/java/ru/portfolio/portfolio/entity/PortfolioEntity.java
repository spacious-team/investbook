package ru.portfolio.portfolio.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "portfolio")
@Data
public class PortfolioEntity {
    @Id
    @Column(name = "portfolio")
    private String portfolio;

    @Override
    public int hashCode() {
        return portfolio.hashCode();
    }
}

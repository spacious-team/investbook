package ru.portfolio.portfolio.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "portfolio_property")
@Data
public class PortfolioPropertyEntity {

    @Id
    @GenericGenerator(name = "UseExistingOrGenerateIdGenerator", strategy = "ru.portfolio.portfolio.entity.UseExistingOrGenerateIdGenerator")
    @GeneratedValue(generator = "UseExistingOrGenerateIdGenerator")
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio", referencedColumnName = "id")
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private PortfolioEntity portfolio;

    @Basic
    @Column(name = "timestamp")
    private Instant timestamp;

    @Basic
    @Column(name = "property")
    private String property;

    @Basic
    @Column(name = "value")
    private String value;
}

package ru.portfolio.portfolio.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "event_cash_flow")
@Data
public class EventCashFlowEntity {
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type", referencedColumnName = "id")
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private CashFlowTypeEntity cashFlowType;

    @Basic
    @Column(name = "value")
    private BigDecimal value;

    @Basic
    @Column(name = "currency")
    private String currency = "RUR";

    @Basic
    @Column(name = "description")
    private String description;

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}

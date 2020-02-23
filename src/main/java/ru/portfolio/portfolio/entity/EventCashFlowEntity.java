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

    @Basic
    @Column(name = "timestamp")
    private Instant timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "isin", referencedColumnName = "isin")
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private SecurityEntity security;

    @Basic
    @Column(name = "count")
    private Integer count;

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

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}

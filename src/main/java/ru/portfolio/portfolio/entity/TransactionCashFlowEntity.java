package ru.portfolio.portfolio.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transaction_cash_flow")
@Data
public class TransactionCashFlowEntity {
    @EmbeddedId
    private TransactionCashFlowEntityPK TransactionCashFlowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private TransactionEntity transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private CashFlowTypeEntity type;

    @Basic
    @Column(name = "value")
    private BigDecimal value;

    @Basic
    @Column(name = "currency")
    private String currency = "RUR";
}

package ru.portfolio.portfolio.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;

@Entity
@Table(name = "transaction")
@Data
public class TransactionEntity {
    @Id
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "isin", referencedColumnName = "ticker")
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private SecurityEntity security;

    @Basic
    @Column(name = "timestamp")
    private Timestamp timestamp;

    @Basic
    @Column(name = "count")
    private int count;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Set<TransactionCashFlowEntity> transactionCashFlows;
}

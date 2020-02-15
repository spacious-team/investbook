package ru.portfolio.portfolio.entity;

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
    @JoinColumn(name = "ticker", referencedColumnName = "ticker")
    private SecurityEntity security;

    @Basic
    @Column(name = "timestamp")
    private Timestamp timestamp;

    @Basic
    @Column(name = "count")
    private int count;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TransactionCashFlowEntity> transactionCashFlows;
}

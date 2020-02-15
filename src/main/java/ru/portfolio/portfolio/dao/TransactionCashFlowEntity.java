package ru.portfolio.portfolio.dao;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "transaction_cash_flow")
@IdClass(TransactionCashFlowEntityPK.class)
@Data
public class TransactionCashFlowEntity {

    @EmbeddedId
    private TransactionCashFlowEntityPK TransactionCashFlowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", referencedColumnName = "id")
    private TransactionEntity transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type", referencedColumnName = "id")
    private CashFlowTypeEntity type;

    @Basic
    @Column(name = "value")
    private int value;

    @Basic
    @Column(name = "currency")
    private String currency;
}

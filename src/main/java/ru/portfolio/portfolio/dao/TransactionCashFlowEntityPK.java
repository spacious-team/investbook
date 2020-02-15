package ru.portfolio.portfolio.dao;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Embeddable
@Table(name = "transaction_cash_flow")
@Data
public class TransactionCashFlowEntityPK implements Serializable {
    @Id
    @Column(name = "transaction_id")
    private int transactionId;

    @Id
    @Column(name = "type")
    private int type;
}

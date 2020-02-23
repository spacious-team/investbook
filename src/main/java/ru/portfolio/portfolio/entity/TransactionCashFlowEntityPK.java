package ru.portfolio.portfolio.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Table;
import java.io.Serializable;

@Embeddable
@Table(name = "transaction_cash_flow")
@Data
public class TransactionCashFlowEntityPK implements Serializable {
    @Column(name = "transaction_id")
    private long transactionId;

    @Column(name = "type")
    private int type;
}

package ru.portfolio.portfolio.entity;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;


@Entity
@Table(name = "event_cash_flow")
@Data
public class EventCashFlowEntity {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Basic
    @Column(name = "timestamp")
    private Timestamp timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker", referencedColumnName = "ticker")
    private SecurityEntity security;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type", referencedColumnName = "id")
    private CashFlowTypeEntity cashFlowType;

    @Basic
    @Column(name = "value")
    private int value;

    @Basic
    @Column(name = "currency")
    private String currency;
}

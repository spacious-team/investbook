package ru.portfolio.portfolio.dao;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "cash_flow_type")
@Data
public class CashFlowTypeEntity {
    @Id
    @Column(name = "id")
    private int id;

    @Basic
    @Column(name = "name")
    private String name;
}

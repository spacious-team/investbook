package ru.portfolio.portfolio.dao;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "issuer")
@Data
public class IssuerEntity {
    @Id
    @Column(name = "inn")
    private int inn;

    @Basic
    @Column(name = "name")
    private String name;
}

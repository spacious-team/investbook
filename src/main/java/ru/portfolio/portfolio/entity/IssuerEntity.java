package ru.portfolio.portfolio.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "issuer")
@Data
public class IssuerEntity {
    @Id
    @Column(name = "inn")
    private long inn;

    @Basic
    @Column(name = "name")
    private String name;
}

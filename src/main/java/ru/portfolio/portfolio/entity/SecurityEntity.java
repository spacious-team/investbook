package ru.portfolio.portfolio.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "security")
@Data
public class SecurityEntity {
    @Id
    @Basic
    @Column(name = "isin")
    private String isin;

    @Column(name = "ticker")
    private String ticker;

    @Basic
    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_inn", referencedColumnName = "inn")
    private IssuerEntity issuer;
}

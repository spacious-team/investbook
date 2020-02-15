package ru.portfolio.portfolio.dao;

import javax.persistence.*;

@Entity
@Table(name = "security")
public class SecurityEntity {
    @Id
    @Column(name = "ticker")
    private String ticker;

    @Basic
    @Column(name = "name")
    private String name;

    @Basic
    @Column(name = "isin")
    private String isin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_inn", referencedColumnName = "inn")
    private IssuerEntity issuer;
}

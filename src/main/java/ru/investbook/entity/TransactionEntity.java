/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(name = "transaction")
@Data
@EqualsAndHashCode(of = "id")
@ToString //(exclude = {"transactionCashFlows"})
public class TransactionEntity {

    @Id
    @AssignedOrGeneratedValue
    @Column(name = "id")
    private Integer id;

    @Basic(optional = false)
    @Column(name = "trade_id")
    private String tradeId;

    @Basic(optional = false)
    @Column(name = "portfolio")
    private String portfolio;

//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "portfolio", referencedColumnName = "id", nullable = false)
//    @JsonIgnoreProperties({"hibernateLazyInitializer"})
//    private PortfolioEntity portfolio;

    // https://stackoverflow.com/questions/17987638/hibernate-one-to-one-lazy-loading-optional-false
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private SecurityEntity security;

    @Basic(optional = false)
    @Column(name = "timestamp")
    private Instant timestamp;

    @Basic(optional = false)
    @Column(name = "count")
    private int count;

    /*
    Nowadays not used, commented due to perf issue (LAZY doesn't work with orphanRemoval = true)

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "transaction", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Set<TransactionCashFlowEntity> transactionCashFlows = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void addTransactionCashFlow(TransactionCashFlowEntity cash) {
        this.transactionCashFlows.add(cash);
        cash.setTransaction(this);
    }

    public void removeTransactionCashFlow(TransactionCashFlowEntity cash) {
        this.transactionCashFlows.remove(cash);
        cash.setTransaction(null);
    }
    */
}

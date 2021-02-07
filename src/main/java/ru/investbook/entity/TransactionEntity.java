/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "transaction")
@Data
@EqualsAndHashCode(of = "pk")
@ToString //(exclude = {"transactionCashFlows"})
public class TransactionEntity {

    @EmbeddedId
    private TransactionEntityPK pk;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "portfolio", referencedColumnName = "id")
//    @JsonIgnoreProperties({"hibernateLazyInitializer"})
//    private PortfolioEntity portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security", referencedColumnName = "id")
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private SecurityEntity security;

    @Basic
    @Column(name = "timestamp")
    private Instant timestamp;

    @Basic
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

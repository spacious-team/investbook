/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Entity
@Table(name = "transaction")
@Data
@ToString(exclude = {"transactionCashFlows"})
public class TransactionEntity {

    @EmbeddedId
    private TransactionEntityPK pk;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "portfolio", referencedColumnName = "id")
//    @JsonIgnoreProperties({"hibernateLazyInitializer"})
//    private PortfolioEntity portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "isin", referencedColumnName = "isin")
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private SecurityEntity security;

    @Basic
    @Column(name = "timestamp")
    private Instant timestamp;

    @Basic
    @Column(name = "count")
    private int count;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
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

    @Override
    public int hashCode() {
        return this.getPk().hashCode();
    }
}

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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;

@Entity
@Table(name = "transaction_cash_flow")
@Data
@EqualsAndHashCode(of = "id")
public class TransactionCashFlowEntity {
    @Id
    @GeneratedValue(generator = UseExistingOrGenerateIdGenerator.NAME)
    @GenericGenerator(name = UseExistingOrGenerateIdGenerator.NAME, strategy = UseExistingOrGenerateIdGenerator.STRATEGY)
    @Column(name = "id")
    private Integer id;

    @Basic(optional = false)
    @Column(name = "transaction_id")
    private int transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type", referencedColumnName = "id", nullable = false)
    private CashFlowTypeEntity cashFlowType;

    @Basic(optional = false)
    @Column(name = "value")
    private BigDecimal value;

    @Basic(optional = false)
    @Column(name = "currency")
    private String currency = "RUR";


    /*
    Nowadays not used, commented due to perf issue

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "transaction_id", referencedColumnName = "id", insertable = false, updatable = false),
            @JoinColumn(name = "portfolio", referencedColumnName = "portfolio", insertable = false, updatable = false)
    })
    private TransactionEntity transaction;
    */
}

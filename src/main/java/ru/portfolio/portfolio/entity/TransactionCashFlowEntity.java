/*
 * Portfolio
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

package ru.portfolio.portfolio.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transaction_cash_flow")
@Data
@JsonSerialize(using = TransactionCashFlowEntitySerializer.class)
public class TransactionCashFlowEntity {
    @EmbeddedId
    private TransactionCashFlowEntityPK pk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "transaction_id", referencedColumnName = "id", insertable = false, updatable = false),
            @JoinColumn(name = "portfolio", referencedColumnName = "portfolio", insertable = false, updatable = false)
    })
    private TransactionEntity transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type", referencedColumnName = "id", insertable = false, updatable = false)
    private CashFlowTypeEntity cashFlowType;

    @Basic
    @Column(name = "value")
    private BigDecimal value;

    @Basic
    @Column(name = "currency")
    private String currency = "RUR";

    @Override
    public int hashCode() {
        return this.getPk().hashCode();
    }
}

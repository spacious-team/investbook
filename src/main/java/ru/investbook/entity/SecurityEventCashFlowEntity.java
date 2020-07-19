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
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "security_event_cash_flow")
@Data
public class SecurityEventCashFlowEntity {
    @Id
    @GenericGenerator(name = "UseExistingOrGenerateIdGenerator", strategy = "ru.investbook.entity.UseExistingOrGenerateIdGenerator")
    @GeneratedValue(generator = "UseExistingOrGenerateIdGenerator")
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio", referencedColumnName = "id")
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private PortfolioEntity portfolio;

    @Basic
    @Column(name = "timestamp")
    private Instant timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "isin", referencedColumnName = "isin")
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private SecurityEntity security;

    @Basic
    @Column(name = "count")
    private Integer count;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type", referencedColumnName = "id")
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private CashFlowTypeEntity cashFlowType;

    @Basic
    @Column(name = "value")
    private BigDecimal value;

    @Basic
    @Column(name = "currency")
    private String currency = "RUR";

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}

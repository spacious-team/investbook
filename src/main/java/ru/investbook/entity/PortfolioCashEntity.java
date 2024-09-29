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
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "portfolio_cash")
@Data
public class PortfolioCashEntity {

    @Id
    @AssignedOrGeneratedValue
    @Column(name = "id")
    private Integer id;

    @Basic
    @Column(name = "portfolio", nullable = false)
    private String portfolio;

    @Basic
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Basic
    @Column(name = "market", nullable = false)
    private String market;

    @Basic
    @Column(name = "value", nullable = false)
    private BigDecimal value;

    @Basic
    @Column(name = "currency", nullable = false)
    private String currency;
}

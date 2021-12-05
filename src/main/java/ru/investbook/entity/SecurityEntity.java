/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.spacious_team.broker.pojo.SecurityType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "security")
@Data
@EqualsAndHashCode(of = "id")
public class SecurityEntity {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    SecurityType type;

    @Column(name = "isin")
    private String isin;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "name")
    private String name;
}

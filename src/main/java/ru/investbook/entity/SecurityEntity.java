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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;
import org.spacious_team.broker.pojo.SecurityType;

import java.util.regex.Pattern;

@Entity
@Table(name = "security")
@Data
@EqualsAndHashCode(of = "id")
public class SecurityEntity {

    public static final Pattern isinPattern = Pattern.compile("^[A-Z]{2}[A-Z0-9]{9}[0-9]$");

    @Id
    @GeneratedValue(generator = AssignedOrIdentityGenerator.NAME)
    @GenericGenerator(name = AssignedOrIdentityGenerator.NAME, type = AssignedOrIdentityGenerator.class)
    @Column(name = "id")
    private Integer id;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private SecurityType type;

    @Column(name = "isin")
    private String isin;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "name")
    private String name;
}

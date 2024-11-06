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

package ru.investbook.converter;

import org.spacious_team.broker.pojo.Issuer;
import org.springframework.stereotype.Component;
import ru.investbook.entity.IssuerEntity;

@Component
public class IssuerConverter implements EntityConverter<IssuerEntity, Issuer>  {

    @SuppressWarnings({"nullness", "DataFlowIssue"})
    @Override
    public IssuerEntity toEntity(Issuer issuer) {
        IssuerEntity entity = new IssuerEntity();
        entity.setId(issuer.getId());
        entity.setTaxpayerId(issuer.getTaxpayerId());
        entity.setName(issuer.getName());
        return entity;
    }

    @Override
    public Issuer fromEntity(IssuerEntity entity) {
        return Issuer.builder()
                .id(entity.getId())
                .taxpayerId(entity.getTaxpayerId())
                .name(entity.getName())
                .build();
    }
}

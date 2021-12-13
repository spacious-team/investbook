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

package ru.investbook.converter;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.Security;
import org.springframework.stereotype.Component;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.repository.IssuerRepository;

@Component
@RequiredArgsConstructor
public class SecurityConverter implements EntityConverter<SecurityEntity, Security> {

    private final IssuerRepository issuerRepository;

    @Override
    public SecurityEntity toEntity(Security security) {
        SecurityEntity entity = new SecurityEntity();
        entity.setId(security.getId());
        entity.setType(security.getType());
        entity.setIsin(security.getIsin());
        entity.setTicker(security.getTicker());
        entity.setName(security.getName());
        return entity;
    }

    @Override
    public Security fromEntity(SecurityEntity entity) {
        return Security.builder()
                .id(entity.getId())
                .type(entity.getType())
                .isin(entity.getIsin())
                .ticker(entity.getTicker())
                .name(entity.getName())
                .build();
    }
}

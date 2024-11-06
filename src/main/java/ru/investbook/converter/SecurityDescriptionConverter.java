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

import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.SecurityDescription;
import org.springframework.stereotype.Component;
import ru.investbook.entity.IssuerEntity;
import ru.investbook.entity.SecurityDescriptionEntity;
import ru.investbook.repository.IssuerRepository;

@Component
@RequiredArgsConstructor
public class SecurityDescriptionConverter implements EntityConverter<SecurityDescriptionEntity, SecurityDescription> {

    private final IssuerRepository issuerRepository;

    @Override
    public SecurityDescriptionEntity toEntity(SecurityDescription security) {
        @Nullable IssuerEntity issuerEntity = null;
        @Nullable Integer issuer = security.getIssuer();
        if (issuer != null) {
            issuerEntity = issuerRepository.getReferenceById(issuer);
        }

        SecurityDescriptionEntity entity = new SecurityDescriptionEntity();
        entity.setSecurity(security.getSecurity());
        entity.setSector(security.getSector());
        entity.setIssuer(issuerEntity);
        return entity;
    }

    @Override
    public SecurityDescription fromEntity(SecurityDescriptionEntity entity) {
        @Nullable IssuerEntity issuer = entity.getIssuer();
        return SecurityDescription.builder()
                .security(entity.getSecurity())
                .sector(entity.getSector())
                .issuer((issuer != null) ? issuer.getId() : null)
                .build();
    }
}

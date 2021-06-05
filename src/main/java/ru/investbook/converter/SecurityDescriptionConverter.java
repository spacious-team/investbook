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
        IssuerEntity issuerEntity = null;
        if (security.getIssuer() != null) {
            issuerEntity = issuerRepository.findById(security.getIssuer())
                    .orElseThrow(() -> new IllegalArgumentException("Эмитент c идентификатором не найден: " + security.getIssuer()));
        }

        SecurityDescriptionEntity entity = new SecurityDescriptionEntity();
        entity.setSecurity(security.getSecurity());
        entity.setSector(security.getSector());
        entity.setIssuer(issuerEntity);
        return entity;
    }

    @Override
    public SecurityDescription fromEntity(SecurityDescriptionEntity entity) {
        return SecurityDescription.builder()
                .security(entity.getSecurity())
                .sector(entity.getSector())
                .issuer((entity.getIssuer() != null) ? entity.getIssuer().getId() : null)
                .build();
    }
}

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

package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.IssuerEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.repository.IssuerRepository;

@Component
@RequiredArgsConstructor
public class SecurityConverter implements EntityConverter<SecurityEntity, Security> {

    private final IssuerRepository issuerRepository;

    @Override
    public SecurityEntity toEntity(Security security) {
        IssuerEntity issuerEntity = null;
        if (security.getInn() != null) {
            issuerEntity = issuerRepository.findByInn(security.getInn())
                    .orElseThrow(() -> new IllegalArgumentException("Эмитетнт с ИНН не найден: " + security.getInn()));
        }

        SecurityEntity entity = new SecurityEntity();
        entity.setTicker(security.getTicker());
        entity.setName(security.getName());
        entity.setIsin(security.getIsin());
        entity.setIssuer(issuerEntity);
        return entity;
    }

    @Override
    public Security fromEntity(SecurityEntity entity) {
        return Security.builder()
                .isin(entity.getIsin())
                .ticker(entity.getTicker())
                .name(entity.getName())
                .inn((entity.getIssuer() != null) ? entity.getIssuer().getInn() : null)
                .build();
    }
}

/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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
import org.spacious_team.broker.pojo.SecurityQuote;
import org.springframework.stereotype.Component;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.SecurityQuoteEntity;
import ru.investbook.repository.SecurityRepository;

@Component
@RequiredArgsConstructor
public class SecurityQuoteConverter implements EntityConverter<SecurityQuoteEntity, SecurityQuote> {
    private final SecurityRepository securityRepository;

    @Override
    public SecurityQuoteEntity toEntity(SecurityQuote quote) {
        SecurityEntity securityEntity = securityRepository.findByIsin(quote.getIsin())
                .orElseThrow(() -> new IllegalArgumentException("Ценная бумага с заданным ISIN не найдена: " + quote.getIsin()));


        SecurityQuoteEntity entity = new SecurityQuoteEntity();
        entity.setId(quote.getId());
        entity.setSecurity(securityEntity);
        entity.setTimestamp(quote.getTimestamp());
        entity.setQuote(quote.getQuote());
        entity.setPrice(quote.getPrice());
        entity.setAccruedInterest(quote.getAccruedInterest());
        return entity;
    }

    @Override
    public SecurityQuote fromEntity(SecurityQuoteEntity entity) {
        return SecurityQuote.builder()
                .id(entity.getId())
                .isin(entity.getSecurity().getIsin())
                .timestamp(entity.getTimestamp())
                .quote(entity.getQuote())
                .price(entity.getPrice())
                .accruedInterest(entity.getAccruedInterest())
                .build();
    }
}

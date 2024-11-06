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
import org.spacious_team.broker.pojo.SecurityQuote;
import org.springframework.stereotype.Component;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.SecurityQuoteEntity;
import ru.investbook.repository.SecurityRepository;

@Component
@RequiredArgsConstructor
public class SecurityQuoteConverter implements EntityConverter<SecurityQuoteEntity, SecurityQuote> {
    private final SecurityRepository securityRepository;

    @SuppressWarnings({"nullness", "DataFlowIssue"})
    @Override
    public SecurityQuoteEntity toEntity(SecurityQuote quote) {
        SecurityEntity securityEntity = securityRepository.getReferenceById(quote.getSecurity());

        SecurityQuoteEntity entity = new SecurityQuoteEntity();
        entity.setId(quote.getId());
        entity.setSecurity(securityEntity);
        entity.setTimestamp(quote.getTimestamp());
        entity.setQuote(quote.getQuote());
        entity.setPrice(quote.getPrice());
        entity.setAccruedInterest(quote.getAccruedInterest());
        entity.setCurrency(quote.getCurrency());
        return entity;
    }

    @Override
    public SecurityQuote fromEntity(SecurityQuoteEntity entity) {
        return SecurityQuote.builder()
                .id(entity.getId())
                .security(entity.getSecurity().getId())
                .timestamp(entity.getTimestamp())
                .quote(entity.getQuote())
                .price(entity.getPrice())
                .accruedInterest(entity.getAccruedInterest())
                .currency(entity.getCurrency())
                .build();
    }
}

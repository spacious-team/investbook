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
import org.spacious_team.broker.pojo.AccountProperty;
import org.spacious_team.broker.pojo.AccountPropertyType;
import org.springframework.stereotype.Component;
import ru.investbook.entity.AccountEntity;
import ru.investbook.entity.AccountPropertyEntity;
import ru.investbook.repository.AccountRepository;

@Component
@RequiredArgsConstructor
public class PortfolioPropertyConverter implements EntityConverter<AccountPropertyEntity, AccountProperty> {
    private final AccountRepository accountRepository;

    @SuppressWarnings({"nullness", "DataFlowIssue"})
    @Override
    public AccountPropertyEntity toEntity(AccountProperty property) {
        AccountEntity accountEntity = accountRepository.getReferenceById(property.getAccount());

        AccountPropertyEntity entity = new AccountPropertyEntity();
        entity.setId(property.getId());
        entity.setAccount(accountEntity);
        entity.setTimestamp(property.getTimestamp());  // when is null, default value is set
        entity.setProperty(property.getProperty().name());
        entity.setValue(property.getValue());
        return  entity;
    }

    @Override
    public AccountProperty fromEntity(AccountPropertyEntity entity) {
        return AccountProperty.builder()
                .id(entity.getId())
                .account(entity.getAccount().getId())
                .timestamp(entity.getTimestamp())
                .property(AccountPropertyType.valueOf(entity.getProperty()))
                .value(entity.getValue())
                .build();
    }
}

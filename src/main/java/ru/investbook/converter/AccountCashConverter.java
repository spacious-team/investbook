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

import org.spacious_team.broker.pojo.AccountCash;
import org.springframework.stereotype.Component;
import ru.investbook.entity.AccountCashEntity;

@Component
public class AccountCashConverter implements EntityConverter<AccountCashEntity, AccountCash>  {

    @SuppressWarnings({"nullness", "DataFlowIssue"})
    @Override
    public AccountCashEntity toEntity(AccountCash cash) {
        AccountCashEntity entity = new AccountCashEntity();
        entity.setId(cash.getId());
        entity.setAccount(cash.getAccount());
        entity.setMarket(cash.getMarket());
        entity.setTimestamp(cash.getTimestamp());
        entity.setValue(cash.getValue());
        entity.setCurrency(cash.getCurrency());
        return entity;
    }

    @Override
    public AccountCash fromEntity(AccountCashEntity entity) {
        return AccountCash.builder()
                .id(entity.getId())
                .account(entity.getAccount())
                .market(entity.getMarket())
                .timestamp(entity.getTimestamp())
                .value(entity.getValue())
                .currency(entity.getCurrency())
                .build();
    }
}

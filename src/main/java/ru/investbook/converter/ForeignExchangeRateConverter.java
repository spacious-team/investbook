/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.springframework.stereotype.Component;
import ru.investbook.entity.ForeignExchangeRateEntity;
import ru.investbook.entity.ForeignExchangeRateEntityPk;

@Component
public class ForeignExchangeRateConverter implements EntityConverter<ForeignExchangeRateEntity, ForeignExchangeRate> {

    @Override
    public ForeignExchangeRateEntity toEntity(ForeignExchangeRate pojo) {
        ForeignExchangeRateEntityPk pk = new ForeignExchangeRateEntityPk();
        pk.setDate(pojo.getDate());
        pk.setCurrencyPair(pojo.getCurrencyPair());
        ForeignExchangeRateEntity entity = new ForeignExchangeRateEntity();
        entity.setPk(pk);
        entity.setRate(pojo.getRate());
        return entity;
    }

    @Override
    public ForeignExchangeRate fromEntity(ForeignExchangeRateEntity entity) {
        return ForeignExchangeRate.builder()
                .date(entity.getPk().getDate())
                .currencyPair(entity.getPk().getCurrencyPair())
                .rate(entity.getRate())
                .build();
    }
}

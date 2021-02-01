/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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
import org.spacious_team.broker.pojo.CashFlowType;
import org.springframework.stereotype.Component;
import ru.investbook.entity.CashFlowTypeEntity;

@Component
@RequiredArgsConstructor
public class CashFlowTypeConverter implements EntityConverter<CashFlowTypeEntity, CashFlowType> {

    @Override
    public CashFlowTypeEntity toEntity(CashFlowType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CashFlowType fromEntity(CashFlowTypeEntity entity) {
        return CashFlowType.valueOf(entity.getId());
    }
}

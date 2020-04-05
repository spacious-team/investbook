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

package ru.portfolio.portfolio.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class TransactionCashFlowEntitySerializer extends StdSerializer<TransactionCashFlowEntity> {

    protected TransactionCashFlowEntitySerializer() {
        super(TransactionCashFlowEntity.class);
    }

    @Override
    public void serialize(TransactionCashFlowEntity object,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("id", object.getPk().getTransactionId());
        jsonGenerator.writeObjectField("portfolio", object.getPk().getPortfolio());
        jsonGenerator.writeObjectField("event-type", object.getPk().getType());
        jsonGenerator.writeObjectField("value", object.getValue());
        jsonGenerator.writeObjectField("currency", object.getCurrency());
        jsonGenerator.writeEndObject();
    }
}

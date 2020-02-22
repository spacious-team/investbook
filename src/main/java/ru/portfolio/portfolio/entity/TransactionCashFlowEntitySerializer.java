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
        jsonGenerator.writeObjectField("transaction-id", object.getTransactionCashFlowId().getTransactionId());
        jsonGenerator.writeObjectField("event-type", object.getTransactionCashFlowId().getType());
        jsonGenerator.writeObjectField("value", object.getValue());
        jsonGenerator.writeObjectField("currency", object.getCurrency());
        jsonGenerator.writeEndObject();
    }
}

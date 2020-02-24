package ru.portfolio.portfolio.pojo;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
public class TransactionCashFlow {
    @NotNull
    @JsonProperty("transaction-id")
    private Long transactionId;

    @NotNull
    @JsonProperty("event-type")
    private CashFlowType eventType;

    @NotNull
    private BigDecimal value;

    @Builder.Default
    private String currency = "RUR";
}

package ru.portfolio.portfolio.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@ToString
@Builder(toBuilder = true)
public class SecurityEventCashFlow {
    @Nullable // autoincrement
    private Integer id;

    @NotNull
    private String portfolio;

    @NotNull
    private Instant timestamp;

    @NotNull
    private String isin;

    @NotNull
    private Integer count;

    @NotNull
    @JsonProperty("event-type")
    private CashFlowType eventType;

    @NotNull
    private BigDecimal value;

    @Builder.Default
    private String currency = "RUR";
}

package ru.portfolio.portfolio.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@ToString
@Builder(toBuilder = true)
public class Transaction {
    @Nullable // autoincrement
    private Long id;

    @NotNull
    private String portfolio;

    @NotNull
    private String isin;

    @NotNull
    private Instant timestamp;

    @NotNull
    private int count;
}

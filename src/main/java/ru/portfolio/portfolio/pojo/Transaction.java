package ru.portfolio.portfolio.pojo;

import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@Builder(toBuilder = true)
public class Transaction {
    @Nullable
    private Integer id;

    @NotNull
    private String isin;

    @NotNull
    private Instant timestamp;

    @NotNull
    private int count;

}

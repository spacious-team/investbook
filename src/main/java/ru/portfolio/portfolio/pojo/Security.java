package ru.portfolio.portfolio.pojo;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;

@Getter
@Builder
public class Security {
    @NotNull
    private final String ticker;

    @NotNull
    private final String name;

    @NotNull
    private final String isin;

    @NotNull
    private final Long inn;
}

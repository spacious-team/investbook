package ru.portfolio.portfolio.pojo;

import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;

@Getter
@Builder(toBuilder = true)
public class Security {
    @NotNull
    private final String isin;

    @Nullable
    private final String ticker;

    @Nullable
    private final String name;

    @Nullable
    private final Long inn;
}

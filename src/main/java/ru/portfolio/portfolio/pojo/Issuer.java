package ru.portfolio.portfolio.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Getter
@ToString
@Builder(toBuilder = true)
public class Issuer {
    @NotNull
    private final Long inn;

    @NotNull
    private final String name;
}

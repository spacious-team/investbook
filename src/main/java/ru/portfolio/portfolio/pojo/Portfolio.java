package ru.portfolio.portfolio.pojo;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;

@Getter
@Builder(toBuilder = true)
public class Portfolio {
    @NotNull
    private final String portfolio;
}

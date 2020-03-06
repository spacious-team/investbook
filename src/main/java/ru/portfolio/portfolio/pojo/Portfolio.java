package ru.portfolio.portfolio.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Getter
@ToString
@Builder(toBuilder = true)
public class Portfolio {
    @NotNull
    private final String portfolio;
}

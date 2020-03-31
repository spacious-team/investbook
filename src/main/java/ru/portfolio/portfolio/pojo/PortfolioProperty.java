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
public class PortfolioProperty {
    @Nullable // autoincrement
    private Integer id;

    @NotNull
    private String portfolio;

    @Nullable
    private Instant timestamp;

    @NotNull
    private PortfolioPropertyType property;

    @NotNull
    private String value;
}

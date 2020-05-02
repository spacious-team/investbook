/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.portfolio.portfolio.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.StringJoiner;

@Getter
@ToString
@Builder(toBuilder = true)
@EqualsAndHashCode
public class EventCashFlow {
    @Nullable // autoincrement
    private Integer id;

    @NotNull
    private String portfolio;

    @NotNull
    private Instant timestamp;

    @NotNull
    @JsonProperty("event-type")
    private CashFlowType eventType;

    @NotNull
    private BigDecimal value;

    @Builder.Default
    private String currency = "RUR";

    @Nullable
    private String description;

    /**
     * Checks DB unique index constraint
     */
    public static boolean checkEquality(EventCashFlow cash1, EventCashFlow cash2) {
        return cash1.getPortfolio().equals(cash2.getPortfolio()) &&
                cash1.getTimestamp().equals(cash2.getTimestamp()) &&
                cash1.getEventType().equals(cash2.getEventType()) &&
                cash1.getValue().equals(cash2.getValue()) &&
                cash1.getCurrency().equals(cash2.getCurrency());
    }

    /**
     * Merge information of two objects with equals by {@link #checkEquality(EventCashFlow, EventCashFlow)}
     */
    public static Collection<EventCashFlow> mergeDuplicates(EventCashFlow cash1, EventCashFlow cash2) {
        StringJoiner joiner = new StringJoiner("; ");
        if (cash1.getDescription() != null) joiner.add(cash1.getDescription());
        if (cash2.getDescription() != null) joiner.add(cash2.getDescription());
        String description = (joiner.length() == 0) ? null : joiner.toString();
        return Collections.singletonList(cash1.toBuilder()
                .value(cash1.getValue().add(cash2.getValue()))
                .description((description == null) ? null : description.substring(0, Math.min(500, description.length())))
                .build());
    }
}

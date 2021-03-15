/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.report;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import ru.investbook.web.ViewFilterModel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.time.ZoneId.systemDefault;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class ViewFilter {
    private static final ThreadLocal<ViewFilter> filters = ThreadLocal.withInitial(() -> null);
    public static final Instant defaultFromDate = Instant.ofEpochSecond(0);
    private static final Function<LocalDate, Instant> toInstant = date -> date.atStartOfDay(systemDefault()).toInstant();

    public static ViewFilter of(ViewFilterModel viewFilterModel, Supplier<? extends Collection<String>> allPortfoliosSupplier) {
        Collection<String> portfolios = viewFilterModel.getPortfolios();
        if (!portfolios.isEmpty()) {
            Collection<String> allPortfolios = allPortfoliosSupplier.get();
            if (portfolios.size() == allPortfolios.size() && portfolios.containsAll(allPortfolios)) {
                // portfolio filter not required
                portfolios = Collections.emptySet();
            }
        }
        return ViewFilter.builder()
                .fromDate(toInstant.apply(viewFilterModel.getFromDate()))
                .toDate(toInstant.apply(viewFilterModel.getToDate()).plus(1, ChronoUnit.DAYS).minusSeconds(1))
                .portfolios(portfolios)
                .build();
    }

    @Builder.Default
    private final Instant fromDate = defaultFromDate;

    @Builder.Default
    private final Instant toDate = Instant.now();

    /**
     * Show all portfolios if empty
     */
    @Builder.Default
    private final Collection<String> portfolios = Collections.emptySet();

    public static void set(ViewFilter viewFilter) {
        filters.set(viewFilter);
    }

    public static ViewFilter get() {
        return filters.get();
    }

    public static void remove() {
        filters.remove();
    }
}

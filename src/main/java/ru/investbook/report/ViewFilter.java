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

import java.time.Instant;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class ViewFilter {
    private static final ThreadLocal<ViewFilter> filters = ThreadLocal.withInitial(() -> null);
    public static final Instant defaultFromDate = Instant.ofEpochSecond(0);

    @Builder.Default
    private final Instant fromDate = defaultFromDate;
    @Builder.Default
    private final Instant toDate = Instant.now();

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

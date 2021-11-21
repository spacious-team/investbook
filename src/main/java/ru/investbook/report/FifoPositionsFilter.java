/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

import org.spacious_team.broker.pojo.Portfolio;

import java.time.Instant;
import java.util.Collection;

import static java.util.Collections.singleton;

public interface FifoPositionsFilter {

    Collection<String> getPortfolios();

    Instant getFromDate();

    Instant getToDate();

    static FifoPositionsFilter of(Portfolio portfolio) {
        Collection<String> portfolios = singleton(portfolio.getId());
        Instant toDate = Instant.now();
        return new FifoPositionsFilter() {
            public Collection<String> getPortfolios() {
                return portfolios;
            }

            @Override
            public Instant getFromDate() {
                return ViewFilter.defaultFromDate;
            }

            @Override
            public Instant getToDate() {
                return toDate;
            }
        };
    }

    static FifoPositionsFilter of(ViewFilter filter) {
        return new FifoPositionsFilter() {
            public Collection<String> getPortfolios() {
                return filter.getPortfolios();
            }

            @Override
            public Instant getFromDate() {
                return filter.getFromDate();
            }

            @Override
            public Instant getToDate() {
                return filter.getToDate();
            }
        };
    }

    static FifoPositionsFilter of(Portfolio portfolio, Instant from, Instant to) {
        return of(singleton(portfolio.getId()), from, to);
    }

    static FifoPositionsFilter of(Collection<String> portfolios, Instant from, Instant to) {
         return new FifoPositionsFilter() {
            @Override
            public Collection<String> getPortfolios() {
                return portfolios;
            }

            @Override
            public Instant getFromDate() {
                return from;
            }

            @Override
            public Instant getToDate() {
                return to;
            }
        };
    }
}

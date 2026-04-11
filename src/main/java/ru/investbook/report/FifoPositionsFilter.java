/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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

import org.spacious_team.broker.pojo.Account;

import java.time.Instant;
import java.util.Collection;

import static java.util.Collections.singleton;

public interface FifoPositionsFilter {

    Collection<String> getAccounts();

    Instant getFromDate();

    Instant getToDate();

    static FifoPositionsFilter of(Account account) {
        return of(account.getId());
    }

    static FifoPositionsFilter of(String account) {
        Collection<String> accounts = singleton(account);
        Instant toDate = Instant.now();
        return new FifoPositionsFilter() {
            public Collection<String> getAccounts() {
                return accounts;
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
            public Collection<String> getAccounts() {
                return filter.getAccounts();
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

    static FifoPositionsFilter of(Collection<String> accounts) {
        return of(accounts, ViewFilter.defaultFromDate, Instant.now());
    }

    static FifoPositionsFilter of(Account account, Instant from, Instant to) {
        return of(singleton(account.getId()), from, to);
    }

    static FifoPositionsFilter of(String account, Instant from, Instant to) {
        return of(singleton(account), from, to);
    }

    static FifoPositionsFilter of(Collection<String> accounts, Instant from, Instant to) {
         return new FifoPositionsFilter() {
            @Override
            public Collection<String> getAccounts() {
                return accounts;
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

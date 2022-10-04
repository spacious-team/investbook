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

package ru.investbook.service.moex;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static java.lang.String.valueOf;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

@EqualsAndHashCode
@RequiredArgsConstructor
class MoexMarketDescription {
    private final String engine;
    private final String market;
    private final String board;
    @Getter
    private final String currency; // may be null (exactly null for futures, options, currency pairs)

    static MoexMarketDescription of(Map<String, Object> description) {
        String engine = valueOf(requireNonNull(description.get("engine")));
        String market = valueOf(requireNonNull(description.get("market")));
        String board = valueOf(requireNonNull(description.get("boardid")));
        String currency = ofNullable(description.get("currencyid"))
                .map(String::valueOf)
                .orElse(null);
        return new MoexMarketDescription(engine, market, board, currency);
    }

    Map<String, String> toMap() {
        return Map.of(
                "engine", engine,
                "market", market,
                "board", board);
    }
}

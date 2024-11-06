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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityQuote.SecurityQuoteBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * <pre>
 * PREVDATE -           Дата предыдущего торгового дня (все)
 * PREVADMITTEDQUOTE -  Признаваемая котировка предыдущего дня (акции, облигации)
 * PREVSETTLEPRICE -    Расчетная цена предыдущего дня, рублей (срочный контракты)
 * PREVPRICE -          Цена последней сделки предыдущего торгового дня (все)
 * MINSTEP -            Шаг котировки (все)
 * STEPPRICE -       	Стоимость шага цены (только фьючерс, но не опцион)
 * ACCRUEDINT -         НКД (облигации)
 * LOTSIZE -            Размер лота, ц.б. (акции, облигации, валюта)
 * LOTVALUE -           Номинальная стоимость лота, в валюте номинала (акции, облигации)
 * </pre>
 */
@Getter
@Slf4j
@RequiredArgsConstructor
public class MoexSecurityQuoteHelper {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    static Optional<SecurityQuoteBuilder> parse(Map<String, Object> description) {
        try {
            LocalDate date = ofNullable(description.get("PREVDATE"))
                    .map(String::valueOf)
                    .map(value -> LocalDate.parse(value, ISO_LOCAL_DATE))
                    .orElseGet(() -> LocalDate.now().minusDays(1));
            BigDecimal quote = ofNullable(description.get("PREVADMITTEDQUOTE")) // share or bond
                    .or(() -> ofNullable(description.get("PREVSETTLEPRICE"))) // forts
                    .or(() -> ofNullable(description.get("PREVPRICE"))) // currency, share/bond, forts
                    .map(MoexSecurityQuoteHelper::toBigDecimal)
                    .orElseThrow();
            @Nullable BigDecimal accruedInterest = ofNullable(description.get("ACCRUEDINT"))
                    .map(MoexSecurityQuoteHelper::toBigDecimal)
                    .orElse(null);
            @Nullable BigDecimal price = null; // share
            if (accruedInterest != null) { // bond
                price = toBigDecimal(description.get("LOTVALUE"))
                        .divide(toBigDecimal(description.get("LOTSIZE")), 4, RoundingMode.HALF_UP)
                        .multiply(quote)
                        .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            } else if (description.containsKey("STEPPRICE")) { // forts
                price = quote.multiply(toBigDecimal(description.get("STEPPRICE")))
                        .divide(toBigDecimal(description.get("MINSTEP")), 2, RoundingMode.HALF_UP);
            }

            return Optional.of(SecurityQuote.builder()
                    .timestamp(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    .quote(quote)
                    .price(price)
                    .accruedInterest(accruedInterest));
        } catch (Exception e) {
            log.warn("Ответ Moex ISS не содержит сведений о котировке", e);
            return Optional.empty();
        }
    }

    private static BigDecimal toBigDecimal(@Nullable Object value) {
        return BigDecimal.valueOf(
                Double.parseDouble(
                        String.valueOf(
                                requireNonNull(value, "Can't parse value to BigDecimal"))));
    }
}

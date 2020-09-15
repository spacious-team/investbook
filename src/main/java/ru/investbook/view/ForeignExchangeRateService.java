/*
 * InvestBook
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

package ru.investbook.view;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.investbook.pojo.PortfolioPropertyType;
import ru.investbook.repository.PortfolioPropertyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForeignExchangeRateService {
    private static final ZoneId moexTimezone = ZoneId.of("Europe/Moscow");
    private final PortfolioPropertyRepository portfolioPropertyRepository;
    // base-currency -> quote-currency -> exchange-rate
    private final Map<String, Map<String, BigDecimal>> cache = new ConcurrentHashMap<>();
    // base-currency -> quote-currency -> local date -> exchange-rate
    private final Map<String, Map<String, Map<LocalDate, BigDecimal>>> cacheByDate = new ConcurrentHashMap<>();

    /**
     * Возвращает котировку базовой валюты в цене котируемой валюты. Например, для USD/RUB базовая валюта - USD.
     *
     * @param baseCurrency  базовая валюта
     * @param quoteCurrency котируемая валюта
     * @return обменный курс валюты в российских рублях
     */
    public BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency) {
        if (baseCurrency.equalsIgnoreCase(quoteCurrency)) {
            return BigDecimal.ONE;
        }
        BigDecimal exchangeRate = getFromCache(baseCurrency, quoteCurrency);
        if (exchangeRate != null) {
            return exchangeRate;
        } else if (baseCurrency.equalsIgnoreCase("RUB")) {
            exchangeRate = BigDecimal.ONE.divide(getExchangeRateToRub(quoteCurrency), 6, RoundingMode.HALF_UP);
        } else if (quoteCurrency.equalsIgnoreCase("RUB")) {
            exchangeRate = getExchangeRateToRub(baseCurrency);
        } else {
            BigDecimal baseToRub = getExchangeRateToRub(baseCurrency);
            BigDecimal quoteToRub = getExchangeRateToRub(quoteCurrency);
            exchangeRate = baseToRub.divide(quoteToRub, 6, RoundingMode.HALF_UP);
        }
        cache(baseCurrency, quoteCurrency, exchangeRate);
        return exchangeRate;
    }

    /**
     * Возвращает котировку базовой валюты в цене котируемой валюты. Например, для USD/RUB базовая валюта - USD.
     *
     * @param currency базовая валюта
     * @return обменный курс валюты в российских рублях
     */
    public BigDecimal getExchangeRateToRub(String currency) {
        if (currency.equalsIgnoreCase("rub")) {
            return BigDecimal.ONE;
        }
        PortfolioPropertyType property = getExchangePropertyFor(currency);
        BigDecimal exchangeRate = portfolioPropertyRepository
                .findFirstByPropertyOrderByTimestampDesc(property.name())
                .map(v -> BigDecimal.valueOf(Double.parseDouble(v.getValue())))
                .orElse(BigDecimal.ZERO);

        if (exchangeRate.equals(BigDecimal.ZERO)) {
            exchangeRate = getDefaultExchangeRate(currency);
        }
        cache(currency, "RUB", exchangeRate);
        return exchangeRate;
    }

    /**
     * Возвращает котировку базовой валюты в цене котируемой валюты. Например, для USD/RUB базовая валюта - USD.
     *
     * @param baseCurrency  базовая валюта
     * @param quoteCurrency котируемая валюта
     * @param instant       на момент времени
     * @param timezone      в точке Земного шара с заданной таймзоной
     * @return обменный курс валюты в российских рублях
     */
    public BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency, Instant instant, ZoneId timezone) {
        if (baseCurrency.equalsIgnoreCase(quoteCurrency)) {
            return BigDecimal.ONE;
        }
        LocalDate localDate = LocalDate.ofInstant(instant, timezone);
        BigDecimal exchangeRate = getFromCache(baseCurrency, quoteCurrency, localDate);
        if (exchangeRate != null) {
            return exchangeRate;
        } else if (baseCurrency.equalsIgnoreCase("RUB")) {
            exchangeRate = BigDecimal.ONE.divide(getExchangeRateToRub(quoteCurrency, instant, timezone),
                    6, RoundingMode.HALF_UP);
        } else if (quoteCurrency.equalsIgnoreCase("RUB")) {
            exchangeRate = getExchangeRateToRub(baseCurrency, instant, timezone);
        } else {
            BigDecimal baseToRub = getExchangeRateToRub(baseCurrency, instant, timezone);
            BigDecimal quoteToRub = getExchangeRateToRub(quoteCurrency, instant, timezone);
            exchangeRate = baseToRub.divide(quoteToRub, 6, RoundingMode.HALF_UP);
        }
        cache(baseCurrency, quoteCurrency, localDate, exchangeRate);
        return exchangeRate;
    }

    /**
     * Возвращает котировку базовой валюты в цене котируемой валюты. Например, для USD/RUB базовая валюта - USD.
     *
     * @param currency базовая валюта
     * @param instant  на момент времени
     * @param timezone в точке Земного шара с заданной таймзоной
     * @return обменный курс валюты в российских рублях
     */
    public BigDecimal getExchangeRateToRub(String currency, Instant instant, ZoneId timezone) {
        if (currency.equalsIgnoreCase("rub")) {
            return BigDecimal.ONE;
        }
        PortfolioPropertyType property = getExchangePropertyFor(currency);
        LocalDate localDate = LocalDate.ofInstant(instant, timezone);
        ZonedDateTime dayStart = localDate.atStartOfDay(timezone);
        BigDecimal exchangeRate = portfolioPropertyRepository
                .findByPropertyAndTimestampBetweenOrderByTimestampDesc(
                        property.name(),
                        dayStart.toInstant(),
                        dayStart.plusDays(1).minusNanos(1).toInstant())
                .stream()
                .findFirst()
                .map(v -> BigDecimal.valueOf(Double.parseDouble(v.getValue())))
                .orElse(BigDecimal.ZERO);

        if (exchangeRate.equals(BigDecimal.ZERO)) {
            exchangeRate = getDefaultExchangeRate(currency);
        }
        cache(currency, "RUB", localDate, exchangeRate);
        return exchangeRate;
    }

    public static PortfolioPropertyType getExchangePropertyFor(String currency) {
        return PortfolioPropertyType.valueOf(currency.toUpperCase() + "RUB_EXCHANGE_RATE");
    }

    private void cache(String baseCurrency, String quoteCurrency, BigDecimal exchangeRate) {
        this.cache.computeIfAbsent(baseCurrency, k -> new ConcurrentHashMap<>())
                .putIfAbsent(quoteCurrency, exchangeRate);
    }

    private void cache(String baseCurrency, String quoteCurrency, LocalDate localDate, BigDecimal exchangeRate) {
        this.cacheByDate.computeIfAbsent(baseCurrency, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(quoteCurrency, k -> new ConcurrentHashMap<>())
                .putIfAbsent(localDate, exchangeRate);
    }

    private BigDecimal getFromCache(String baseCurrency, String quoteCurrency) {
        return this.cache.computeIfAbsent(baseCurrency, k -> new ConcurrentHashMap<>())
                .get(quoteCurrency);
    }

    private BigDecimal getFromCache(String baseCurrency, String quoteCurrency, LocalDate localDate) {
        return this.cacheByDate.computeIfAbsent(baseCurrency, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(quoteCurrency, k -> new ConcurrentHashMap<>())
                .get(localDate);

    }

    private static BigDecimal getDefaultExchangeRate(String currency) {
        BigDecimal exchangeRate;
        exchangeRate = switch (currency) {
            case "EUR" -> BigDecimal.valueOf(90);
            case "GBP" -> BigDecimal.valueOf(100);
            default -> BigDecimal.valueOf(80);
        };
        log.debug("Не могу в БД найти курс валюты {}, использую значение по умолчанию = {}",
                currency, exchangeRate);
        return exchangeRate;
    }
}

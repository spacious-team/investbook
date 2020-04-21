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

package ru.portfolio.portfolio.view;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.portfolio.portfolio.pojo.PortfolioPropertyType;
import ru.portfolio.portfolio.repository.PortfolioPropertyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForeignExchangeRateService {
    private final PortfolioPropertyRepository portfolioPropertyRepository;
    // base-currency -> quote-currency -> exchange-rate
    private final Map<String, Map<String, BigDecimal>> cache = new ConcurrentHashMap<>();

    /**
     * Возвращает котировку базовой валюты в цене котируемой валюты. Например, для USD/RUB базовая валюта - USD.
     * @param baseCurrency базовая валюта
     * @param quoteCurrency котируемая валюта
     */
    public BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency) {
        BigDecimal exchangeRate = getFromCache(baseCurrency, quoteCurrency);
        if (exchangeRate != null) {
            return exchangeRate;
        } else if (baseCurrency.equalsIgnoreCase("RUB")) {
            exchangeRate = BigDecimal.ONE.divide(getExchangeRateToRub(quoteCurrency), 6, RoundingMode.HALF_UP);
        } else if (quoteCurrency.equalsIgnoreCase("RUB")) {
            exchangeRate = getExchangeRateToRub(baseCurrency);
        } else {
            BigDecimal base = getExchangeRateToRub(baseCurrency);
            BigDecimal quote = getExchangeRateToRub(quoteCurrency);
            exchangeRate = base.divide(quote, 6, RoundingMode.HALF_UP);
        }
        cache(baseCurrency, quoteCurrency, exchangeRate);
        return exchangeRate;
    }

    /**
     * @return exchange rate for currency (russian rubles)
     */
    public BigDecimal getExchangeRateToRub(String currency) {
        PortfolioPropertyType property = getExchangePropertyFor(currency);
        BigDecimal exchangeRate = portfolioPropertyRepository
                .findFirstByPropertyOrderByTimestampDesc(property.name())
                .map(v -> BigDecimal.valueOf(Double.parseDouble(v.getValue())))
                .orElse(BigDecimal.ZERO);

        if (exchangeRate.equals(BigDecimal.ZERO)) {
            exchangeRate = BigDecimal.valueOf(75);
            switch (currency) {
                case "EUR":
                    exchangeRate = BigDecimal.valueOf(85);
                    break;
                case "GBP":
                    exchangeRate = BigDecimal.valueOf(95);
                    break;
            }
            log.debug("Не могу в БД найти курс валюты {}, использую значение по умолчанию = {}",
                    currency, exchangeRate);
        }
        cache(currency, "RUB", exchangeRate);
        return exchangeRate;
    }

    public static PortfolioPropertyType getExchangePropertyFor(String currency) {
        return PortfolioPropertyType.valueOf(currency.toUpperCase() + "RUB_EXCHANGE_RATE");
    }

    private void cache(String baseCurrency, String quoteCurrency, BigDecimal exchangeRate) {
        this.cache.computeIfAbsent(baseCurrency, k -> new ConcurrentHashMap<>())
                .putIfAbsent(quoteCurrency, exchangeRate);
    }

    private BigDecimal getFromCache(String baseCurrency, String quoteCurrency) {
        return this.cache.computeIfAbsent(baseCurrency, k -> new ConcurrentHashMap<>())
                .get(quoteCurrency);
    }
}

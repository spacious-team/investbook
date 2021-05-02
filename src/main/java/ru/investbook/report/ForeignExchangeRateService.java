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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityQuote.SecurityQuoteBuilder;
import org.spacious_team.broker.pojo.SecurityType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.investbook.entity.ForeignExchangeRateEntity;
import ru.investbook.repository.ForeignExchangeRateRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.spacious_team.broker.pojo.SecurityType.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class ForeignExchangeRateService {
    public static final String RUB = "RUB";
    private final ForeignExchangeRateRepository foreignExchangeRateRepository;
    // base-currency -> quote-currency -> exchange-rate
    private final Map<String, Map<String, BigDecimal>> cache = new ConcurrentHashMap<>();
    // base-currency -> quote-currency -> local date -> exchange-rate
    private final Map<String, Map<String, Map<LocalDate, BigDecimal>>> cacheByDate = new ConcurrentHashMap<>();
    @Value("${server.port}")
    private int serverPort;

    /**
     * Возвращает последнюю известную котировку базовой валюты в цене котируемой валюты. Например, для USD/RUB базовая валюта - USD.
     *
     * @param baseCurrency  базовая валюта
     * @param quoteCurrency котируемая валюта
     * @return обменный курс в цене котируемой валюты
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
     * Возвращает последнюю известную котировку базовой валюты в цене котируемой валюты. Например, для USD/RUB базовая валюта - USD.
     *
     * @param currency базовая валюта
     * @return обменный курс валюты в российских рублях
     */
    public BigDecimal getExchangeRateToRub(String currency) {
        if (currency.equalsIgnoreCase("rub")) {
            return BigDecimal.ONE;
        }
        BigDecimal exchangeRate = foreignExchangeRateRepository
                .findFirstByPkCurrencyPairOrderByPkDateDesc(currency.toUpperCase() + "RUB")
                .map(ForeignExchangeRateEntity::getRate)
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
     * @param atDate        на заданную дату
     * @return обменный курс в цене котируемой валюты или, если курс не найден, дефолтное значение
     */
    public BigDecimal getExchangeRateOrDefault(String baseCurrency, String quoteCurrency, LocalDate atDate) {
        try {
            return getExchangeRate(baseCurrency, quoteCurrency, atDate);
        } catch (Exception e) {
            BigDecimal baseToRub = getExchangeRateToRubOrDefault(baseCurrency, atDate);
            BigDecimal quoteToRub = getExchangeRateToRubOrDefault(quoteCurrency, atDate);
            BigDecimal defaultExchangeRate = baseToRub.divide(quoteToRub, 6, RoundingMode.HALF_UP);
            log.warn("Курс валюты {}{} на дату {} не известен, использую ориентировочное значение {}",
                    baseCurrency, quoteCurrency, atDate, defaultExchangeRate);
            return defaultExchangeRate;
        }
    }

    /**
     * Возвращает котировку базовой валюты в цене котируемой валюты. Например, для USD/RUB базовая валюта - USD.
     *
     * @param baseCurrency  базовая валюта
     * @param quoteCurrency котируемая валюта
     * @param atDate        на заданную дату
     * @return обменный курс в цене котируемой валюты
     * @throws NoSuchElementException если обменный курс не известен
     */
    public BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency, LocalDate atDate) {
        if (baseCurrency.equalsIgnoreCase(quoteCurrency)) {
            return BigDecimal.ONE;
        }
        BigDecimal exchangeRate = getFromCache(baseCurrency, quoteCurrency, atDate);
        if (exchangeRate != null) {
            return exchangeRate;
        } else if (baseCurrency.equalsIgnoreCase("RUB")) {
            exchangeRate = BigDecimal.ONE.divide(getExchangeRateToRub(quoteCurrency, atDate), 6, RoundingMode.HALF_UP);
        } else if (quoteCurrency.equalsIgnoreCase("RUB")) {
            exchangeRate = getExchangeRateToRub(baseCurrency, atDate);
        } else {
            BigDecimal baseToRub = getExchangeRateToRub(baseCurrency, atDate);
            BigDecimal quoteToRub = getExchangeRateToRub(quoteCurrency, atDate);
            exchangeRate = baseToRub.divide(quoteToRub, 6, RoundingMode.HALF_UP);
        }
        cache(baseCurrency, quoteCurrency, atDate, exchangeRate);
        return exchangeRate;
    }

    /**
     * Возвращает котировку базовой валюты в цене котируемой валюты. Например, для USD/RUB базовая валюта - USD.
     *
     * @param currency базовая валюта
     * @param atDate   на заданную дату
     * @return обменный курс валюты в российских рублях или, если курс не найден, дефолтное значение
     */
    public BigDecimal getExchangeRateToRubOrDefault(String currency, LocalDate atDate) {
        try {
            return getExchangeRateToRub(currency, atDate);
        } catch (Exception e) {
            BigDecimal defaultExchangeRate = getDefaultExchangeRate(currency);
            log.warn("Курс валюты {}RUB на дату {} не известен, использую ориентировочное значение {}",
                    currency.toUpperCase(), atDate, defaultExchangeRate);
            return defaultExchangeRate;
        }
    }

    /**
     * Возвращает котировку базовой валюты в цене котируемой валюты. Например, для USD/RUB базовая валюта - USD.
     *
     * @param currency базовая валюта
     * @param atDate   на заданную дату
     * @return обменный курс валюты в российских рублях
     * @throws NoSuchElementException если обменный курс не известен
     */
    public BigDecimal getExchangeRateToRub(String currency, LocalDate atDate) {
        if (currency.equalsIgnoreCase("rub")) {
            return BigDecimal.ONE;
        }
        BigDecimal exchangeRate = foreignExchangeRateRepository
                .findByPkCurrencyPairAndPkDate(currency.toUpperCase() + "RUB", atDate)
                .map(ForeignExchangeRateEntity::getRate)
                .orElse(BigDecimal.ZERO);

        if (exchangeRate.equals(BigDecimal.ZERO)) {
            throw new NoSuchElementException("Курс валюты " + currency.toUpperCase() + "RUB на дату " + atDate +
                    " не известен, загрузите значение курса с сайта ЦБ РФ (https://www.cbr.ru/currency_base/dynamics/) " +
                    " запросом POST http://localhost:" + serverPort + "/foreign-exchange-rates");
        }
        cache(currency, "RUB", atDate, exchangeRate);
        return exchangeRate;
    }

    public void invalidateCache() {
        this.cache.clear();
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

    /**
     * Конвертирует сумму денежных средств из заданной валюты в целевую валюту по последнему известному курсу или
     * курсу по-умолчанию, если официальный курс не известен.
     */
    public BigDecimal convertValueToCurrency(BigDecimal value, String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return value;
        } else {
            BigDecimal exchangeRate = getExchangeRate(fromCurrency, toCurrency);
            return value.multiply(exchangeRate);
        }
    }

    /**
     * Конвертирует котировку из заданной валюты в целевую валюту по последнему известному курсу или
     * курсу по-умолчанию, если официальный курс не известен.
     */
    public SecurityQuote convertQuoteToCurrency(SecurityQuote quote, String toCurrency) {
        String fromCurrency = quote.getCurrency();
        if (fromCurrency == null || fromCurrency.equalsIgnoreCase(toCurrency)) {
            return quote;
        } else {
            return convertQuoteToCurrency(quote, () -> getExchangeRate(fromCurrency, toCurrency))
                    .currency(toCurrency)
                    .build();
        }
    }

    private SecurityQuoteBuilder convertQuoteToCurrency(SecurityQuote quote, Supplier<BigDecimal> exchangeRateSupplier) {
        BigDecimal exchangeRate = exchangeRateSupplier.get();
        BigDecimal accruedInterest = quote.getAccruedInterest();
        SecurityType type = getSecurityType(quote.getSecurity());
        boolean nonCurrencyQuote = ((type == STOCK_OR_BOND) && (accruedInterest != null)) || (type == DERIVATIVE);
        return quote.toBuilder()
                .quote(nonCurrencyQuote ? quote.getQuote() : quote.getQuote().multiply(exchangeRate))
                .price((quote.getPrice() == null) ? null : quote.getPrice().multiply(exchangeRate))
                .accruedInterest((accruedInterest == null) ? null : accruedInterest.multiply(exchangeRate));
    }
}

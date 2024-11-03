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

package ru.investbook.parser;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import org.springframework.stereotype.Component;
import ru.investbook.report.ForeignExchangeRateService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionValueAndFeeParser {

    private final ZoneId moexZoneId = ZoneId.of("Europe/Moscow");
    private final ForeignExchangeRateService foreignExchangeRateService;

    public Arguments.ArgumentsBuilder argumentsBuilder() {
        return Arguments.builder()
                .exchangeRateProvider(this::defaultExchangeRate);
    }

    /**
     * Суммирует значения из колонок комиссии с учетом валюты и ее курса на день сделки между собой
     * или, если это не возможно, вычитает комиссии из стоимости сделки
     *
     * @throws RuntimeException если невозможно просуммировать комиссии между собой или вычесть из суммы сделки
     */
    public Result parse(Arguments arg) {

        @Nullable BigDecimal brokerFee = getFee(arg.row, arg.brokerFeeColumn).orElse(null);
        @Nullable BigDecimal marketFee = getFee(arg.row, arg.marketFeeColumn).orElse(null);
        @Nullable BigDecimal clearingFee = getFee(arg.row, arg.clearingFeeColumn).orElse(null);
        @Nullable BigDecimal stampDuty = getFee(arg.row, arg.stampDutyColumn).orElse(null);


        BigDecimal value = arg.value;
        String valueCurrency = filterCurrency(arg.row.getStringCellValue(arg.valueCurrencyColumn));
        BigDecimal totalFee = BigDecimal.ZERO;
        String totalFeeCurrency = getTotalFeeCurrency(arg, valueCurrency, brokerFee, marketFee, clearingFee, stampDuty);

        try {
            totalFee = addToTotalFee(totalFee, totalFeeCurrency,
                    brokerFee, arg.brokerFeeCurrencyColumn, arg); // add to total fee ...
        } catch (Exception ignore) {
            value = subtractFromValue(value, valueCurrency, brokerFee, arg.brokerFeeCurrencyColumn, arg); // ... or to value
        }

        try {
            totalFee = addToTotalFee(totalFee, totalFeeCurrency,
                    marketFee, arg.marketFeeCurrencyColumn, arg);
        } catch (Exception ignore) {
            value = subtractFromValue(value, valueCurrency, marketFee, arg.marketFeeCurrencyColumn, arg);
        }

        try {
            totalFee = addToTotalFee(totalFee, totalFeeCurrency,
                    clearingFee, arg.clearingFeeCurrencyColumn, arg);
        } catch (Exception ignore) {
            value = subtractFromValue(value, valueCurrency, clearingFee, arg.clearingFeeCurrencyColumn, arg);
        }

        try {
            totalFee = addToTotalFee(totalFee, totalFeeCurrency,
                    stampDuty, arg.stampDutyCurrencyColumn, arg);
        } catch (Exception ignore) {
            value = subtractFromValue(value, valueCurrency, stampDuty, arg.stampDutyCurrencyColumn, arg);
        }

        return new Result(value, valueCurrency, totalFee, totalFeeCurrency);
    }

    private Optional<BigDecimal> getFee(TableRow row, @Nullable TableHeaderColumn feeColumn) {
        //noinspection DataFlowIssue,ReturnOfNull
        return Optional.ofNullable(feeColumn)
                .map(col -> row.getBigDecimalCellValueOrDefault(col, null))
                .map(v -> Math.abs(v.floatValue()) > 1e-3 ? v : null);
    }

    private String getTotalFeeCurrency(Arguments arg, String valueCurrency,
                                       @Nullable BigDecimal brokerFee,
                                       @Nullable BigDecimal marketFee,
                                       @Nullable BigDecimal clearingFee,
                                       @Nullable BigDecimal stampDuty) {
        //noinspection ConstantConditions
        List<String> feeCurrencies =
                Stream.of(
                                (brokerFee == null) ? null : arg.brokerFeeCurrencyColumn,
                                (marketFee == null) ? null : arg.marketFeeCurrencyColumn,
                                (clearingFee == null) ? null : arg.clearingFeeCurrencyColumn,
                                (stampDuty == null) ? null : arg.stampDutyCurrencyColumn)
                        .filter(Objects::nonNull)
                        .map(col -> arg.row.getStringCellValueOrDefault(col, null))
                        .filter(Objects::nonNull)
                        .map(this::filterCurrency)
                        .distinct()
                        .toList();
        if (feeCurrencies.isEmpty()) {
            return valueCurrency;
        } else if (feeCurrencies.size() == 1) {
            return feeCurrencies.getFirst();
        }
        List<String> currencies = feeCurrencies.stream()
                .filter(currency -> !Objects.equals(currency, valueCurrency))
                .toList();
        if (currencies.size() == 1) {
            return currencies.getFirst();
        }
        throw new IllegalArgumentException("Три разные валюты при проведении сделки не поддерживаются");
    }

    private String filterCurrency(String currency) {
        return ("RUR".equalsIgnoreCase(currency) ? "RUB" : currency).toUpperCase();
    }

    /**
     * @throws NoSuchElementException если обменный курс не известен
     */
    private BigDecimal addToTotalFee(BigDecimal totalFee, String totalFeeCurrency,
                                     @Nullable BigDecimal fee, @Nullable TableHeaderColumn feeCurrencyColumn,
                                     Arguments arg) {
        if (fee != null && feeCurrencyColumn != null) {
            String feeCurrency = filterCurrency(arg.row.getStringCellValue(feeCurrencyColumn));
            BigDecimal exchangeRate = arg.exchangeRateProvider
                    .getExchangeRate(feeCurrency, totalFeeCurrency, arg.transactionInstant);
            return totalFee.add(fee.multiply(exchangeRate));
        }
        return totalFee;
    }

    private BigDecimal subtractFromValue(BigDecimal value, String valueCurrency,
                                         @Nullable BigDecimal fee, @Nullable TableHeaderColumn feeCurrencyColumn,
                                         Arguments arg) {
        if (fee == null || feeCurrencyColumn == null) {
            return value;
        }
        String feeCurrency = filterCurrency(arg.row.getStringCellValue(feeCurrencyColumn));
        if (valueCurrency.equalsIgnoreCase(feeCurrency)) {
            log.warn("""
                    Сделка {} счета {} имеет комиссии в разных валютах, обменный курс не известен на дату сделки, \
                    поэтому часть комиссии ({} {}), совпадающей с валютой сделки ({}), включена в сумму сделки ({} {})
                    """, arg.tradeId, arg.portfolio, fee, feeCurrency, valueCurrency, value, valueCurrency);
            return value.subtract(fee);
        }
        throw new IllegalArgumentException("Не удалось сохранить комиссию сделки, валюта комиссии. Сделка " +
                arg.tradeId + " счета " + arg.portfolio + " имеет комиссии в разных валютах, " +
                "обменный курс не известен на дату сделки.");
    }

    /**
     * @param fee положительное для списания комиссии
     */
    public record Result(BigDecimal value, String valueCurrency, BigDecimal fee, String feeCurrency) {
    }

    @Value
    @Builder
    @SuppressWarnings("type.anno.before.modifier")
    public static class Arguments {
        TableRow row;
        String portfolio;
        String tradeId;
        Instant transactionInstant;
        ExchangeRateProvider exchangeRateProvider;
        BigDecimal value; // отрицательное - для покупки, положительное - для продажи
        TableHeaderColumn valueCurrencyColumn;
        @Nullable
        TableHeaderColumn brokerFeeColumn; // положительное значение для списания комиссии
        @Nullable
        TableHeaderColumn brokerFeeCurrencyColumn;
        @Nullable
        TableHeaderColumn marketFeeColumn; // положительное значение для списания комиссии
        @Nullable
        TableHeaderColumn marketFeeCurrencyColumn;
        @Nullable
        TableHeaderColumn clearingFeeColumn; // положительное значение для списания комиссии
        @Nullable
        TableHeaderColumn clearingFeeCurrencyColumn;
        @Nullable
        TableHeaderColumn stampDutyColumn; // Гербовый сбор Гонконгской биржи, положительное значение для списания комиссии
        @Nullable
        TableHeaderColumn stampDutyCurrencyColumn;
    }

    public interface ExchangeRateProvider {

        /**
         * Возвращает котировку базовой валюты в цене котируемой валюты. Например, для USD/RUB базовая валюта - USD.
         *
         * @param baseCurrency       базовая валюта
         * @param quoteCurrency      котируемая валюта
         * @param transactionInstant на заданное время
         * @return обменный курс в цене котируемой валюты
         * @throws NoSuchElementException если обменный курс не известен
         */
        BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency, Instant transactionInstant);

    }

    private BigDecimal defaultExchangeRate(String baseCurrency, String quoteCurrency, Instant transactionInstant) {
        return foreignExchangeRateService.getExchangeRate(
                baseCurrency, quoteCurrency, LocalDate.ofInstant(transactionInstant, moexZoneId));
    }
}

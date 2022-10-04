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

package ru.investbook.service.cbr;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.converter.ForeignExchangeRateConverter;
import ru.investbook.entity.ForeignExchangeRateEntity;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.repository.ForeignExchangeRateRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractCbrForeignExchangeRateService implements CbrForeignExchangeRateService {

    private final Map<String, String> currencyParamValues = Map.of(
            "USD", "R01235",
            "EUR", "R01239",
            "GBP", "R01035",
            "CHF", "R01775");
    private final ForeignExchangeRateRepository foreignExchangeRateRepository;
    private final ForeignExchangeRateConverter foreignExchangeRateConverter;

    @Override
    @Transactional
    @SneakyThrows
    public void updateFrom(LocalDate fromDate) {
        long t0 = System.nanoTime();
        try (ForkJoinPool pool = new ForkJoinPool(currencyParamValues.size())) {
            pool.submit(() -> currencyParamValues.entrySet()
                            .parallelStream()
                            .forEach(e -> updateCurrencyRate(fromDate, e)))
                    .get();
        }
        log.info("Курсы валют обновлены за {}", Duration.ofNanos(System.nanoTime() - t0));
    }

    private void updateCurrencyRate(LocalDate fromDate, Map.Entry<String, String> e) {
        try {
            long t0 = System.nanoTime();
            String currency = e.getKey();
            String currencyPair = currency.toUpperCase() + ForeignExchangeRateService.RUB;
            updateCurrencyRate(currencyPair, e.getValue(), fromDate);
            log.info("Курс {} обновлен за {}", currencyPair, Duration.ofNanos(System.nanoTime() - t0));
        } catch (Exception ex) {
            throw new RuntimeException("Не смог обновить курсы валют", ex);
        }
    }

    protected abstract void updateCurrencyRate(String currencyPair, String currencyId, LocalDate fromDate);

    protected void save(ForeignExchangeRate fxRate) {
        try {
            ForeignExchangeRateEntity entity = foreignExchangeRateConverter.toEntity(fxRate);
            foreignExchangeRateRepository.save(entity);
        } catch (Exception e) {
            log.debug("Ошибка сохранения {}, может быть запись уже существует?", fxRate);
        }
    }
}

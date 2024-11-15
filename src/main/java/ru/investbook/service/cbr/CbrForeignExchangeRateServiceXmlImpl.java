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

import generated.ValCurs;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import lombok.SneakyThrows;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import ru.investbook.converter.ForeignExchangeRateConverter;
import ru.investbook.repository.ForeignExchangeRateRepository;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class CbrForeignExchangeRateServiceXmlImpl extends AbstractCbrForeignExchangeRateService {

    @SuppressWarnings("HttpUrlsUsage")
    private final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(
            "http://www.cbr.ru/scripts/XML_dynamic.asp?date_req1={from-date}&date_req2={to-date}&VAL_NM_RQ={currency}");
    private final DateTimeFormatter requestDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter resultDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public CbrForeignExchangeRateServiceXmlImpl(ForeignExchangeRateRepository foreignExchangeRateRepository,
                                                ForeignExchangeRateConverter foreignExchangeRateConverter) {
        super(foreignExchangeRateRepository, foreignExchangeRateConverter);
    }

    @Override
    @SneakyThrows
    protected void updateCurrencyRate(String currencyPair, String currencyId, LocalDate fromDate) {
        getFxRates(fromDate, currencyId)
                .getRecord()
                .stream()
                .map(record -> getRate(record, currencyPair))
                .forEach(this::save);
    }

    private ValCurs getFxRates(LocalDate fromDate, String currencyId) throws JAXBException, IOException {
        try (InputStream stream = getInputStream(fromDate, currencyId)) {

            return (ValCurs) JAXBContext.newInstance(ValCurs.class)
                    .createUnmarshaller()
                    .unmarshal(stream);
        }
    }

    private InputStream getInputStream(LocalDate fromDate, String currencyId) throws IOException {
        return uriBuilder.buildAndExpand(Map.of(
                        "currency", currencyId,
                        "from-date", fromDate.format(requestDateFormatter),
                        "to-date", LocalDate.now().format(requestDateFormatter)))
                .toUri()
                .toURL()
                .openStream();
    }

    private ForeignExchangeRate getRate(ValCurs.Record record, String currencyPair) {
        return ForeignExchangeRate.builder()
                .date(LocalDate.parse(record.getDate(), resultDateFormatter))
                .currencyPair(currencyPair)
                .rate(BigDecimal.valueOf(parseDouble(record.getValue()))
                        .divide(BigDecimal.valueOf(record.getNominal()), 4, RoundingMode.HALF_UP))
                .build();
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            if (value.indexOf(',') != -1) {
                return Double.parseDouble(value.replace(',', '.'));
            } else if (value.indexOf('.') != -1) {
                return Double.parseDouble(value.replace('.', ','));
            }
            throw e;
        }
    }
}

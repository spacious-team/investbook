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

package ru.investbook.service.moex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.spacious_team.broker.pojo.SecurityType.*;

/**
 * <b>Steps for getting security quote:</b>
 * <pre>
 *
 * http://iss.moex.com/iss/securities?q=Si-6.21
 *
 *     -> Search: secid
 *
 * http://iss.moex.com/iss/securities/AAL-RM        (stock)
 * http://iss.moex.com/iss/securities/RU000A0JWSQ7  (bond)
 * http://iss.moex.com/iss/securities/SiM1          (future/option)
 * http://iss.moex.com/iss/securities/USD000UTSTOM  (currency pair)
 *
 *   -> Search for is_primary = 1: engine, market and board
 *
 * http://iss.moex.com/iss/engines/stock/markets/foreignshares/boards/FQBR/securities/AAL-RM.json?iss.meta=off&iss.only=securities&securities.columns=SECID,PREVDATE,PREVADMITTEDQUOTE
 * http://iss.moex.com/iss/engines/stock/markets/foreignshares/boards/FQBR/securities/RU000A0JWSQ7.json?iss.meta=off&iss.only=securities&securities.columns=SECID,PREVDATE,PREVADMITTEDQUOTE,ACCRUEDINT,LOTSIZE,LOTVALUE
 * http://iss.moex.com/iss/engines/futures/markets/forts/boards/RFUD/securities/SiM1.json?iss.meta=off&iss.only=securities&securities.columns=SECID,PREVDATE,PREVSETTLEPRICE,MINSTEP,STEPPRICE
 * http://iss.moex.com/iss/engines/currency/markets/selt/boards/CETS/securities/USD000UTSTOM.json?iss.meta=off&iss.only=securities&securities.columns=SECID,PREVDATE,PREVPRICE
 *
 *   -> Parse json with quote
 *      Columns description available on page http://iss.moex.com/iss/engines/{engine}/markets/{market}
 *
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MoexIssClientImpl implements MoexIssClient {

    private static final String securitiesUri = "http://iss.moex.com/iss/securities.json?" +
            "iss.meta=off&" +
            "securities.columns=secid,shortname,isin&" +
            "start=0&" +
            "limit=10&" +
            "q={query}";
    private static final String securityUri = "http://iss.moex.com/iss/securities/{secId}.json?" +
            "iss.only=boards&" +
            "boards.columns=is_primary,engine,market,boardid";
    private static final String quoteUri = "http://iss.moex.com/iss/engines/{engine}/markets/{market}/boards/{board}/securities/{secId}.json?" +
            "iss.meta=off&" +
            "iss.only=securities&" +
            "securities.columns=SECID,PREVDATE,PREVADMITTEDQUOTE,PREVSETTLEPRICE,PREVPRICE,ACCRUEDINT,LOTSIZE,LOTVALUE,MINSTEP,STEPPRICE";
    private final MoexDerivativeSecidHelper moexDerivativeSecidHelper;
    private final RestTemplate restTemplate;
    private int currentYear = getCurrentYear();
    private long fastCoarseDayCounter = getFastCoarseDayCounter();

    @Override
    public Optional<String> getSecId(String isinOrContractName) {
        SecurityType securityType = getSecurityType(isinOrContractName);
        if (securityType == DERIVATIVE) {
            // Moex couldn't find futures contract (too many records). Try evaluate contract name
            Optional<String> secid = moexDerivativeSecidHelper.getFuturesContractSecidIfCan(isinOrContractName);
            if (secid.isPresent()) {
                return secid;
            }
        }
        // Moex couldn't find contract  (shortname=USDRUB_TOM) by USDRUB_TOM, but finds it by USDRUB
        String query = (securityType == CURRENCY_PAIR) ? getCurrencyPair(isinOrContractName) : isinOrContractName;
        return Optional.ofNullable(restTemplate.getForObject(securitiesUri, Map.class, query))
                .map(MoexJsonResponseParser::buildFromIntObjectMap)
                .flatMap(securities -> securities.stream()
                        .filter(security -> isinOrContractName.equals(security.get("isin")) ||
                                isinOrContractName.equals(security.get("shortname")) ||
                                isinOrContractName.equals(security.get("secid")))
                        .map(s -> s.get("secid"))
                        .map(String::valueOf)
                        .findAny());
    }

    @Override
    public Optional<MoexMarketDescription> getMarket(String moexSecId) {
        return Optional.ofNullable(restTemplate.getForObject(securityUri, Map.class, moexSecId))
                .map(MoexJsonResponseParser::buildFromIntObjectMap)
                .flatMap(response -> response.stream()
                        .filter(m -> Integer.valueOf(1).equals(m.get("is_primary")))
                        .map(MoexMarketDescription::of)
                        .findAny());
    }

    public Optional<SecurityQuote> getQuote(String moexSecId, MoexMarketDescription market) {
        Map<String, String> variables = new HashMap<>(market.toMap());
        variables.put("secId", moexSecId);
        Optional<SecurityQuote> quote = Optional.ofNullable(restTemplate.getForObject(quoteUri, Map.class, variables))
                .map(MoexJsonResponseParser::buildFromIntObjectMap)
                .flatMap(_quote -> _quote.stream()
                        .findAny()
                        .flatMap(MoexSecurityQuoteHelper::parse));
        if (quote.isPresent() && moexDerivativeSecidHelper.isSecidPossibleOption(moexSecId)) {
            // Котировка опциона не содержит цену SecurityQuote.price,
            // т.к. ИСС МосБиржи, определяя MINSTEP, не сообщает STEPPRICE.
            // STEPPRICE нужно получить из базового актива (фьючерса)
            return moexDerivativeSecidHelper.getOptionUnderlingFuturesContract(moexSecId)
                    .filter(moexDerivativeSecidHelper::isFutures)
                    .flatMap(underlyingSecid -> getMarket(underlyingSecid)
                            .flatMap(underlyingMarket -> getQuote(underlyingSecid, underlyingMarket)))
                    .map(futuresContract -> futuresContract.getPrice()
                            .divide(futuresContract.getQuote(), 6, RoundingMode.HALF_UP))
                    .map(oneUnitPrice -> quote.get().getQuote().multiply(oneUnitPrice))
                    .map(optionalPrice -> quote.get().toBuilder()
                                .price(optionalPrice)
                                .build())
                    .or(() -> quote); // не удалось вычислить, возвращаем без SecurityQuote.price
        }
        return quote;
    }

    public boolean isDerivativeAndExpired(String shortnameOrSecid) {
        try {
            SecurityType securityType = getSecurityType(shortnameOrSecid);
            if (securityType == DERIVATIVE) {
                int currentYear = getCurrentYear();
                int year;
                // only current plus 2 years contracts may have quotes
                int dashPos = shortnameOrSecid.indexOf('-');
                if (dashPos == -1) {
                    // last number (last o penultimate symbol) contains year
                    // (SiM1, Si75000BD1, Si75000BD1A)
                    try {
                        year = Integer.parseInt(shortnameOrSecid.substring(shortnameOrSecid.length() - 1));
                    } catch (Exception e) {
                        year = Integer.parseInt(shortnameOrSecid.substring(shortnameOrSecid.length() - 2));
                    }
                    year = currentYear / 10 * 10 + year;
                } else {
                    // 2-digits year after '.' (Si-6.21, Si-6.21M170621CA75000)
                    int dotPos = shortnameOrSecid.indexOf('.');
                    if (dotPos == -1) {
                        return false;
                    }
                    year = Integer.parseInt(shortnameOrSecid.substring(dotPos + 1, dotPos + 3));
                    year = currentYear / 100 * 100 + year;
                }
                return (year < currentYear) || (year > (currentYear + 2));
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    private int getCurrentYear() {
        if (fastCoarseDayCounter != getFastCoarseDayCounter()) {
            fastCoarseDayCounter = getFastCoarseDayCounter();
            currentYear = LocalDate.now().getYear();
        }
        return currentYear;
    }

    private static long getFastCoarseDayCounter() {
        return System.currentTimeMillis() >>> 26; // increments each 18,6 hours
    }
}

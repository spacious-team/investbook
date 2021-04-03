/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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
import org.springframework.web.util.UriComponents;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.spacious_team.broker.pojo.SecurityType.*;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

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

    private final UriComponents securitiesUri = fromHttpUrl("http://iss.moex.com/iss/securities.json?" +
            "iss.meta=off&" +
            "securities.columns=secid,shortname,isin&" +
            "start=0&" +
            "limit=10&" +
            "q={query}")
            .build();
    private final UriComponents securityUri = fromHttpUrl("http://iss.moex.com/iss/securities/{secId}.json?" +
            "iss.only=boards&" +
            "boards.columns=is_primary,engine,market,boardid").build();
    private final UriComponents quoteUri = fromHttpUrl("http://iss.moex.com/iss/engines/{engine}/markets/{market}/boards/{board}/securities/{secId}.json?" +
            "iss.meta=off&" +
            "iss.only=securities&" +
            "securities.columns=SECID,PREVDATE,PREVADMITTEDQUOTE,PREVSETTLEPRICE,PREVPRICE,ACCRUEDINT,LOTSIZE,LOTVALUE,MINSTEP,STEPPRICE")
            .build();
    private final MoexDerivativeShortnameConvertor moexDerivativeShortnameConvertor;
    private final RestTemplate restTemplate;

    @Override
    public Optional<String> getSecId(String isinOrContractName) {
        SecurityType securityType = getSecurityType(isinOrContractName);
        if (securityType == DERIVATIVE) {
            // Moex couldn't find futures contract (too many records). Try evaluate contract name
            Optional<String> secid = moexDerivativeShortnameConvertor.getFuturesContractSecidIfCan(isinOrContractName);
            if (secid.isPresent()) {
                return secid;
            }
        }
        // Moex couldn't find contract  (shortname=USDRUB_TOM) by USDRUB_TOM, but finds it by USDRUB
        String query = (securityType == CURRENCY_PAIR) ? getCurrencyPair(isinOrContractName) : isinOrContractName;
        URI uri = securitiesUri.expand(query).toUri();
        return Optional.ofNullable(restTemplate.getForObject(uri, Map.class))
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
        URI uri = securityUri.expand(moexSecId).toUri();
        return Optional.ofNullable(restTemplate.getForObject(uri, Map.class))
                .map(MoexJsonResponseParser::buildFromIntObjectMap)
                .flatMap(response -> response.stream()
                        .filter(m -> Integer.valueOf(1).equals(m.get("is_primary")))
                        .map(MoexMarketDescription::of)
                        .findAny());
    }

    public Optional<SecurityQuote> getQuote(String moexSecId, MoexMarketDescription market) {
        Map<String, String> variables = new HashMap<>(market.toMap());
        variables.put("secId", moexSecId);
        URI uri = quoteUri.expand(variables).toUri();
        return Optional.ofNullable(restTemplate.getForObject(uri, Map.class))
                .map(MoexJsonResponseParser::buildFromIntObjectMap)
                .flatMap(quote -> quote.stream()
                        .findAny()
                        .flatMap(MoexSecurityQuoteHelper::parse));
    }
}

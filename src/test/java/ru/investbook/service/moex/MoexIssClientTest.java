/*
 * InvestBook
 * Copyright (C) 2024  Spacious Team <spacious-team@ya.ru>
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class MoexIssClientTest {

    @Mock
    MoexDerivativeCodeService moexDerivativeCodeService;
    RestTemplate restTemplate = new RestTemplate();
    MoexIssClientImpl moexIssClient;

    @BeforeEach
    public void setUp() {
        moexIssClient = new MoexIssClientImpl(moexDerivativeCodeService, restTemplate);
    }

    @ParameterizedTest
    @MethodSource("secids")
    public void getMarket(String secid) {
        Optional<MoexMarketDescription> result = moexIssClient.getMarket(secid);
        Assertions.assertTrue(result.isPresent());
    }

    @ParameterizedTest
    @MethodSource("secids")
    public void getIsin(String secid) {
        Optional<String> result = moexIssClient.getIsin(secid);
        Assertions.assertTrue(result.isPresent());
    }

    @ParameterizedTest
    @MethodSource("secids")
    public void getQuote(String secid) {
        Optional<MoexMarketDescription> market = moexIssClient.getMarket(secid);
        if (market.isPresent()) {
            Optional<SecurityQuote> result = moexIssClient.getQuote(secid, market.get());
            Assertions.assertTrue(result.isPresent());
        }
    }

    public String[] secids() {
        return new String[] {"AFLT", "OZON", "MGNT", "MVID"};
    }
}

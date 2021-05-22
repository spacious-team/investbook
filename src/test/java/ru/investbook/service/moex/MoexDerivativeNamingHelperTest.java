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

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.testng.Assert.assertEquals;

@ExtendWith(MockitoExtension.class)
public class MoexDerivativeNamingHelperTest {

    @Spy
    MoexDerivativeNamingHelper helper;

    static Object[][] getFuturesCodes() {
        return new Object[][] {
                {"Si-6.21", "SiM1"},
                {"RTS-12.19", "RIZ9"},
                {"SiM1", "SiM1"},
                {"RIZ9", "RIZ9"},
                {"BR-5.21", "BRK1"},
                {"BRK1", "BRK1"},
                {"Si65000BC9D", null},
                {"abc", null},
                {"Si-6.211", null},
                {"SI-6.211", null},
                {"Si-13.21", null},
                {"Si-0.21", null},
        };
    }

    @ParameterizedTest
    @MethodSource("getFuturesCodes")
    void getFuturesCode(String shortName, String code) {
        assertEquals(helper.getFuturesCode(shortName).orElse(null), code);
    }

    @ParameterizedTest
    @MethodSource("getFuturesCodes")
    void isFuturesTest(String shortName, String code) {
        assertEquals(helper.isFutures(shortName), code != null);
    }


    static Object[][] getOptionUnderlingFutures() {
        return new Object[][] {
                {"Si75000BL1", "SiZ1"},
                {"Si75000BL1D", "SiZ1"},
                {"RI150000BG9", "RIN9"},
                {"RI150000BS9", "RIN9"},
                {"RI150000BG9A", "RIN9"},
                {"RI150000BS9B", "RIN9"},
                {"RI15000000BS9B", "RIN9"},
                {"BR50BE1", "BRK1"},
                {"BR50BE1A", "BRK1"},
                {"Ri150000BS9B", null},
                {"RI150000B9", null},
                {"RI150000S9B", null},
                {"RIBS9B", null},
                {"RI1a5BS9", null},
                {"abc", null},
                {"SiZ1", null},
                {"Si-6.21", null}
        };
    }

    @ParameterizedTest
    @MethodSource("getOptionUnderlingFutures")
    void getOptionUnderlingFuturesTest(String optionCode, String futuresCode) {
        assertEquals(helper.getOptionUnderlingFutures(optionCode).orElse(null), futuresCode);
    }
}
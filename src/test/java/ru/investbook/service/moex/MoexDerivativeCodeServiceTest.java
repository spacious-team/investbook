/*
 * InvestBook
 * Copyright (C) 2021  Spacious Team <spacious-team@ya.ru>
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
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class MoexDerivativeCodeServiceTest {

    @InjectMocks
    MoexDerivativeCodeService service;

    static Object[][] contractToFuturesCode() {
        return new Object[][]{
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
                {"Si-0.21", null}
        };
    }

    @ParameterizedTest
    @MethodSource("contractToFuturesCode")
    void getFuturesCode(String contract, String expectedCode) {
        Optional<String> code = service.getFuturesCode(contract);

        if (expectedCode == null) {
            assertTrue(code.isEmpty());
        } else {
            assertTrue(code.isPresent());
            assertEquals(code.get(), expectedCode);
        }
    }


    @ParameterizedTest
    @MethodSource("contractToFuturesCode")
    void isFutures1(String contract, String code) {
        boolean isFuturesContract = (code != null);
        assertEquals(service.isFutures(contract), isFuturesContract);
    }

    @ParameterizedTest
    @MethodSource("contractToFuturesCode")
    void isFuturesCode(String contract, String code) {
        boolean isFuturesContract = (code != null);
        //noinspection ConstantConditions
        assertEquals(service.isFuturesCode(code), isFuturesContract);
    }

    static Object[][] contractToFuturesShortName() {
        return new Object[][]{
                {"SiM1", "Si-6.21"},
                {"RIZ9", "RTS-12.19"},
                {"SRX1", "SBRF-11.21"},
                {"SRV2", "SBRF-10.22"},
                {"SPQ9", "SBPR-8.19"},
                {"BRK3", "BR-5.23"},
                {"Si-6.21", "Si-6.21"},
                {"RTS-12.19", "RTS-12.19"},
                {"BR-5.21", "BR-5.21"},
                {"Si65000BC9D", null},
                {"abc", null},
                {"Si-6.211", null},
                {"SI-6.211", null},
                {"Si-13.21", null},
                {"Si-0.21", null},
                {"BRE1", null}
        };
    }

    @ParameterizedTest
    @MethodSource("contractToFuturesShortName")
    void getFuturesShortname(String contract, String expectedShortName) {
        Optional<String> shortName = service.getFuturesShortname(contract);

        if (expectedShortName == null) {
            assertTrue(shortName.isEmpty());
        } else {
            assertTrue(shortName.isPresent());
            assertEquals(shortName.get(), expectedShortName);
        }
    }

    @ParameterizedTest
    @MethodSource("contractToFuturesShortName")
    void isFutures2(String contract, String shortName) {
        boolean isFuturesContract = (shortName != null);
        assertEquals(service.isFutures(contract), isFuturesContract);
    }

    @ParameterizedTest
    @MethodSource("contractToFuturesShortName")
    void isFuturesShortName(String contract, String shortName) {
        boolean isFuturesContract = (shortName != null);
        //noinspection ConstantConditions
        assertEquals(service.isFuturesShortname(shortName), isFuturesContract);
    }

    static Object[][] contractToOptionShortName() {
        return new Object[][]{
                {"BR-7.16M270616CA 50", "BR-7.16M270616CA 50"},
                {"BR50BF6", "BR-7.16M270616CA 50"},
                {"BR50BE1", "BR-6.21M250521CA50"},
                {"BR50BE1A", "BR-6.21M060521CA50"},
                {"LK6200CL3", "LKOHP201223CE6200"},
                {"GZ300CG2D", "GAZPP220722CE 300"},
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
    @MethodSource("contractToOptionShortName")
    void isOption1(String contract, String shortname) {
        boolean isOptionContract = (shortname != null);
        assertEquals(service.isOption(contract), isOptionContract);
    }

    @ParameterizedTest
    @MethodSource("contractToOptionShortName")
    void isOptionShortName(String contract, String shortname) {
        boolean isOptionContract = (shortname != null);
        //noinspection ConstantConditions
        assertEquals(service.isOptionShortname(shortname), isOptionContract);
    }

    static Object[][] optionCodeToFuturesCode() {
        return new Object[][]{
                {"Si75000BL1", "SiZ1"},
                {"Si75000BL0D", "SiH1"},
                {"RI150000BG9", "RIU9"},
                {"RI150000BS9", "RIU9"},
                {"RI150000BG9A", "RIU9"},
                {"RI150000BS9B", "RIU9"},
                {"RI90000BS9", "RIU9"},
                {"BR50BE1", "BRM1"},
                {"BR50BE1A", "BRM1"},
                {"BR50BF1", "BRN1"},
                //{"BR-10BE0", "BRM0"}, // пока только пример из документации МосБиржи
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
    @MethodSource("optionCodeToFuturesCode")
    void isOption2(String optionCode, String futuresCode) {
        boolean isOptionCode = (futuresCode != null);
        assertEquals(service.isOption(optionCode), isOptionCode);
    }

    @ParameterizedTest
    @MethodSource("optionCodeToFuturesCode")
    void isOptionCode(String optionCode, String futuresCode) {
        boolean isOptionCode = (futuresCode != null);
        assertEquals(service.isOptionCode(optionCode), isOptionCode);
    }
}
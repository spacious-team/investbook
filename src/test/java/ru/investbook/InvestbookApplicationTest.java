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

package ru.investbook;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testng.Assert.assertNotNull;

@SpringBootTest(classes = InvestbookApplication.class)
@AutoConfigureMockMvc
class InvestbookApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mvc;

    @DisplayName("Should load ApplicationContext")
    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @DisplayName("Should get actuators status OK")
    @ParameterizedTest
    @CsvSource({
            "info, build.artifact, investbook",
            "health, status, UP",
            "configprops, contexts.application.beans.investbookProperties.prefix, investbook",
    })
    @SneakyThrows
    void shouldHaveActuatorHandler(String actuatorPath, String jsonPath, String expected) {
        mvc.perform(get("http://localhost/actuator/" + actuatorPath))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + jsonPath).value(expected));
    }

}

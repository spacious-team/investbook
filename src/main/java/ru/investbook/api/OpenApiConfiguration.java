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

package ru.investbook.api;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public GroupedOpenApi apiOpenApi() {
        return GroupedOpenApi.builder()
                .group("api")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi actuatorOpenApi(WebEndpointProperties webEndpointProperties) {
        // Обычно "/actuator" — стандартный базовый путь
        String actuatorBasePath = webEndpointProperties.getBasePath();
        return GroupedOpenApi.builder()
                .group("actuator")
                .pathsToMatch(actuatorBasePath + "/**")
                .build();
    }

    @Bean
    public GroupedOpenApi appOpenApi(WebEndpointProperties webEndpointProperties) {
        // Обычно "/actuator" — стандартный базовый путь
        String actuatorBasePath = webEndpointProperties.getBasePath();
        return GroupedOpenApi.builder()
                .group("app")
                .pathsToMatch("/**")
                .pathsToExclude("/api/**", actuatorBasePath + "/**")
                .build();
    }
}

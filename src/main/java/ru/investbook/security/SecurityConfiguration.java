/*
 * InvestBook
 * Copyright (C) 2026  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Optional;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final SecurityCorsProperties securityCorsProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // TODO добавить защиту от CSRF атак: csrf-токен (паттерн "Double Submit Cookie") или JWT c Access и Refresh токенами
                .csrf(AbstractHttpConfigurer::disable)
                // TODO сделать авторизацию для приложения
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        Optional.ofNullable(securityCorsProperties.getAllowedOrigins())
                .ifPresent(configuration::setAllowedOrigins);
        Optional.ofNullable(securityCorsProperties.getAllowedMethods())
                .ifPresent(configuration::setAllowedMethods);
        Optional.ofNullable(securityCorsProperties.getAllowedHeaders())
                .ifPresent(configuration::setAllowedHeaders);
        Optional.ofNullable(securityCorsProperties.getExposedHeaders())
                .ifPresent(configuration::setExposedHeaders);
        Optional.ofNullable(securityCorsProperties.getExposedHeaders())
                .ifPresent(configuration::setExposedHeaders);
        Optional.ofNullable(securityCorsProperties.getAllowCredentials())
                .ifPresent(configuration::setAllowCredentials);
        Optional.ofNullable(securityCorsProperties.getMaxAge())
                .ifPresent(configuration::setMaxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

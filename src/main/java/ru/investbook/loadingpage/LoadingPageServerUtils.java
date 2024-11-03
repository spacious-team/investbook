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

package ru.investbook.loadingpage;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

@Slf4j
@UtilityClass
public class LoadingPageServerUtils {

    private static final String CONF_PROPERTIES = "application-conf.properties";

    public static int getMainAppPort() {
        try {
            Properties properties = loadProperties();
            String value = properties.getProperty("server.port", "2030");
            return Integer.parseInt(value);
        } catch (Exception e) {
            log.warn("Can't find 'server.port' property, fallback to default value: 2030");
            return 2030;
        }
    }

    public static boolean shouldOpenHomePageAfterStart() {
        try {
            Properties properties = loadProperties();
            String value = properties.getProperty("investbook.open-home-page-after-start", "true");
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            log.warn("Can't find 'investbook.open-home-page-after-start' fallback to default value: true");
            return true;
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        Path path = Path.of(CONF_PROPERTIES);
        try (Reader reader = Files.newBufferedReader(path)) {  // default is UTF_8
            properties.load(reader);
        } catch (Exception e) {
            // Properties file is not found in app installation path, read default file from class path
            try (InputStream in = requireNonNull(LoadingPageServerUtils.class.getResourceAsStream(CONF_PROPERTIES));
                 Reader reader = new InputStreamReader(in, UTF_8)) {
                properties.load(reader);
            }
        }
        return properties;
    }
}

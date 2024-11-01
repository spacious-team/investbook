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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

@UtilityClass
public class LoadingPageServerUtils {

    public static final String CONF_PROPERTIES = "src/main/resources/application-conf.properties";

    public static Properties loadProperties(Path filePath) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(filePath.toFile())) {
            properties.load(input);
        }
        return properties;
    }

    public static int getMainAppPort() {
        Properties properties;
        try {
            properties = loadProperties(Path.of(CONF_PROPERTIES));
        } catch (IOException e) {
            return 2030;
        }
        return Integer.parseInt(properties.getProperty("server.port", "2030"));
    }

    public static boolean shouldOpenHomePageAfterStart() {
        Properties properties;
        try {
            properties = loadProperties(Path.of(CONF_PROPERTIES));
        } catch (IOException e) {
            return true;
        }
        return Boolean.parseBoolean(properties.getProperty("investbook.open-home-page-after-start", "true"));
    }
}

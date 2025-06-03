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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

@Slf4j
@UtilityClass
public class LoadingPageHttpServerUtils {

    private static final String PROPERTIES_LOCATION_DIR = "app";
    private static final String DEFAULT_PROFILE = "conf";
    private static final List<String> profiles = new CopyOnWriteArrayList<>();

    static void setSpringProfilesFromArgs(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--spring.profiles.active")) {
                String[] _profiles = arg.replace("--spring.profiles.active", "")
                        .replace("=", "")
                        .trim()
                        .split(",");
                profiles.clear();
                profiles.addAll(List.of(_profiles));
                Collections.reverse(profiles);  // last spring profile property should win
                profiles.add(DEFAULT_PROFILE);
            }
        }
    }

    public static int getMainAppPort() {
        try {
            String value = getProperty("server.port", "2030");
            return Integer.parseInt(value);
        } catch (Exception e) {
            log.warn("Can't find 'server.port' property, fallback to default value: 2030", e);
            return 2030;
        }
    }

    public static boolean shouldOpenHomePageAfterStart() {
        try {
            String value = getProperty("investbook.open-home-page-after-start", "true");
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            log.warn("Can't find 'investbook.open-home-page-after-start' fallback to default value: true", e);
            return true;
        }
    }

    private static String getProperty(String key, String defaultValue) {
        for (String profile : profiles) {
            try {
                Properties properties = loadProperties(profile);
                @Nullable String value = properties.getProperty(key, null);
                if (value != null) {
                    return value;
                }
            } catch (Exception ignore) {
            }
        }
        return defaultValue;
    }

    private static Properties loadProperties(String profile) throws IOException {
        Properties properties = new Properties();
        String file = "application-" + profile + ".properties";
        Path path = Path.of(PROPERTIES_LOCATION_DIR).resolve(file);
        try (Reader reader = Files.newBufferedReader(path)) {  // default is UTF_8
            properties.load(reader);
        } catch (Exception e) {
            // Properties file is not found in app installation path, read default file from class path
            try (InputStream in = requireNonNull(LoadingPageHttpServerUtils.class.getResourceAsStream("/" + file));
                 Reader reader = new InputStreamReader(in, UTF_8)) {
                properties.load(reader);
            }
        }
        return properties;
    }
}

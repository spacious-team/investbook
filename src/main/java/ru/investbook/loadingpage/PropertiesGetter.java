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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * Helps to read property value from Environment variable, application run arguments or '.properties' file.
 */
@Slf4j
@UtilityClass
class PropertiesGetter {
    // ./app for msi installation
    private static final String[] PROPERTIES_LOCATION_DIR = {"./", "./config", "./app"};
    private static final String DEFAULT_PROFILE = "conf";
    private static final List<String> args = new CopyOnWriteArrayList<>();
    private static final List<String> profiles = new CopyOnWriteArrayList<>();

    static synchronized void setSpringProfilesFromArgs(String[] _args) {
        args.clear();
        args.addAll(Arrays.asList(_args));
        profiles.add("default");  // allows to read "spring.profiles.active" property from application.properties
        String[] _profiles = getProperty("spring.profiles.active", DEFAULT_PROFILE)
                .split(",");
        profiles.clear();
        profiles.addAll(List.of(_profiles));
        Collections.reverse(profiles);  // last spring profile property should win
        profiles.add("default");  // always read application.properties
    }

    @SuppressWarnings("SameParameterValue")
    static int getIntProperty(String property, int defaultValue) {
        try {
            String value = getProperty(property, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (Exception e) {
            log.warn("Can't find '{}' property, fallback to default value: {}", property, defaultValue, e);
            return defaultValue;
        }
    }

    @SuppressWarnings("SameParameterValue")
    static boolean getBooleanProperty(String property, boolean defaultValue) {
        try {
            String value = getProperty(property, String.valueOf(defaultValue));
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            log.warn("Can't find '{}' property, fallback to default value: {}", property, defaultValue, e);
            return true;
        }
    }

    static String getProperty(String property, String defaultValue) {
        try {
            return readPropertyFromEnv(property)
                    .or(() -> readPropertyFromArgs(property))
                    .or(() -> readPropertyFromFile(property))
                    .orElse(defaultValue);
        } catch (Exception e) {
            log.warn("Can't find '{}' property, fallback to default value: '{}'", property, defaultValue, e);
            return defaultValue;
        }
    }

    private static Optional<String> readPropertyFromEnv(String property) {
        String varName = convertPropertyNameToEnvVarName(property);
        return Optional.ofNullable(System.getenv(varName));
    }

    private static String convertPropertyNameToEnvVarName(String property) {
        return property.toUpperCase()
                .replace("_", "")
                .replace(".", "_");
    }

    private static Optional<String> readPropertyFromArgs(String property) {
        String prefix = "--" + property;
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                String value = arg.replace(prefix, "")
                        .replace("=", "")
                        .trim();
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> readPropertyFromFile(String key) {
        for (String profile : profiles) {
            try {
                Properties properties = loadProperties(profile);
                @Nullable String value = properties.getProperty(key, null);
                if (value != null) {
                    log.trace("Get property {}={}", key, value);
                    return Optional.of(value);
                }
            } catch (Exception e) {
                log.trace("Can't read profile '{}' file", profile, e);
            }
        }
        return Optional.empty();
    }

    private static Properties loadProperties(String profile) throws IOException {
        Properties properties = new Properties();
        String suffix = Objects.equals(profile, "default") ? "" : "-" + profile;
        String file = "application" + suffix + ".properties";
        for (String dir : PROPERTIES_LOCATION_DIR) {
            Path path = Path.of(dir).resolve(file);
            try (Reader reader = Files.newBufferedReader(path)) {  // default is UTF_8
                properties.load(reader);
                log.trace("Read properties '{}' from file {}", profile, path);
                return properties;
            } catch (Exception ignore) {
            }
        }
        // Properties file is not found in app installation path, read default file from class path
        try (InputStream in = requireNonNull(PropertiesGetter.class.getResourceAsStream("/" + file));
             Reader reader = new InputStreamReader(in, UTF_8)) {
            properties.load(reader);
            log.trace("Read properties '{}' from classpath:/{}", profile, file);
            return properties;
        }
    }
}

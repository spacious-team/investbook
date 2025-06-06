/*
 * InvestBook
 * Copyright (C) 2025  Spacious Team <spacious-team@ya.ru>
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

import lombok.SneakyThrows;

import java.lang.reflect.Field;

import static java.util.Objects.requireNonNull;

/**
 * Helps to open loading page when org.springframework.boot:spring-boot-devtools dependency in classpath
 */
public class LoadingPageHttpServerDevToolImpl implements LoadingPageHttpServer {

    private final AutoCloseable loadingPageHttpServer;

    public LoadingPageHttpServerDevToolImpl() {
        this.loadingPageHttpServer = getLoadingPageHttpServerImpl();
    }

    @SneakyThrows
    public static AutoCloseable getLoadingPageHttpServerImpl() {
        // returns "main" thread's ClassLoader's instance
        ClassLoader classLoader = requireNonNull(LoadingPageHttpServer.Implementation.class.getClassLoader());
        ClassLoader parentClassLoader = requireNonNull(classLoader.getParent());
        Class<?> clazz = parentClassLoader.loadClass(LoadingPageHttpServer.Implementation.class.getName());
        Field field = clazz.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        return (AutoCloseable) requireNonNull(field.get(clazz));
    }

    @Override
    public void start() {
        // server already started, do nothing
    }

    @SneakyThrows
    @Override
    public void close() {
        loadingPageHttpServer.close();
    }
}

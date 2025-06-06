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

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Objects;

import static ru.investbook.loadingpage.LoadingPageHttpServer.Implementation.INSTANCE;
import static ru.investbook.loadingpage.LoadingPageHttpServerHelper.setSpringProfilesFromArgs;

public interface LoadingPageHttpServer extends AutoCloseable {

    void start();

    void close();

    static LoadingPageHttpServer of(String[] args) {
        setSpringProfilesFromArgs(args);
        INSTANCE = isDevToolThread() ?
                new LoadingPageHttpServerDevToolImpl() :
                new LoadingPageHttpServerImpl();
        INSTANCE.start();
        return INSTANCE;
    }

    /**
     * @return true is thread started by
     * <a href="https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-devtools">spring-boot-devtools</a>
     */
    private static boolean isDevToolThread() {
        return Objects.equals(Thread.currentThread().getName(), "restartedMain");
    }

    class Implementation {
        protected static volatile @MonotonicNonNull LoadingPageHttpServer INSTANCE;

        public static void close() {
            if (INSTANCE != null) {
                INSTANCE.close();
            }
        }
    }
}

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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Phased;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

/**
 * Stops {@link LoadingPageHttpServer}
 * just before {@link org.springframework.boot.web.servlet.context.WebServerStartStopLifecycle}
 */
@Service
@RequiredArgsConstructor
public class LoadingPageHttpServerService implements SmartLifecycle {
    private final ApplicationContext applicationContext;

    @Getter
    private volatile boolean running = false;

    @SneakyThrows
    @Override
    public void start() {
        @Nullable LoadingPageHttpServer server = LoadingPageHttpServer.getInstance();
        if (!isRunning() && server != null) {
            server.close();
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    @SneakyThrows
    @Override
    public int getPhase() {
        Phased webServerStartStopLifecycle = (Phased) applicationContext.getBean("webServerStartStop");
        return webServerStartStopLifecycle.getPhase() - 1;

    }
}

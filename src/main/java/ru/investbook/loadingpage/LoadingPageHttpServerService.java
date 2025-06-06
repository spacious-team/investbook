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
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Phased;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

/**
 * Stops {@link LoadingPageHttpServerImpl}
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
        if (!isRunning()) {
            LoadingPageHttpServer.Implementation.close();
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    @SneakyThrows
    @Override
    public int getPhase() {
        try {
            Phased webServerStartStopLifecycle = (Phased) applicationContext.getBean("webServerStartStop");
            return webServerStartStopLifecycle.getPhase() - 1;
        } catch (NoSuchBeanDefinitionException e) {
            // there is no WebServerStartStopLifecycle (maybe tests in the progress), phase doesn't matter
            return Integer.MIN_VALUE;
        }
    }
}

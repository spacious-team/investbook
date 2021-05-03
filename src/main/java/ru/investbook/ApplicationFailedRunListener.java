/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

public class ApplicationFailedRunListener implements ApplicationListener<ApplicationFailedEvent> {

    private final Logger log = LoggerFactory.getLogger(ApplicationFailedRunListener.class);

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        ConfigurableEnvironment environment = event.getApplicationContext().getEnvironment();
        if (environment.getProperty("investbook.open-home-page-after-start", Boolean.class, false)) {
            log.info("Application run failed. May be application was run a second time, trying to open the application page...");
            int port = environment.getProperty("server.port", Integer.class, 8080);
            BrowserHomePageOpener.open("http://localhost:" + port);
        }
    }
}

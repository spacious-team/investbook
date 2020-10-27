/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@SpringBootApplication
@Component
@RequiredArgsConstructor
public class InvestbookApplication {

    private final PortfolioProperties properties;

    public static void main(String[] args) {
        try {
            SpringApplication app = new SpringApplication(InvestbookApplication.class);
            app.addListeners(new ApplicationFailedRunListener());
            app.run(args);
        } catch (ApplicationContextException e) {
            // gh-81 do not show "Failed to launch JVM"
            System.exit(0);
        }
    }

    @EventListener
    public void onApplicationEvent(ServletWebServerInitializedEvent event) {
        if (properties.isOpenHomePageAfterStart()) {
            int port = event.getWebServer().getPort();
            BrowserHomePageOpener.open("http://localhost:" + port);
        }
    }
}

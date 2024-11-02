/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContextException;
import org.springframework.stereotype.Component;
import ru.investbook.loadingpage.LoadingPageServer;
import ru.investbook.loadingpage.LoadingPageServerUtils;

@SpringBootApplication
@Component
@EnableCaching
@RequiredArgsConstructor
public class InvestbookApplication {

    public static void main(String[] args) {
        try(LoadingPageServer loadingPageServer = new LoadingPageServer()) {
            if (LoadingPageServerUtils.shouldOpenHomePageAfterStart()) {
                loadingPageServer.start();
            }

            SpringApplication app = new SpringApplication(InvestbookApplication.class);
            app.addListeners(new ApplicationFailedRunListener());
            app.run(args);
        } catch (ApplicationContextException e) {
            // gh-81 do not show "Failed to launch JVM"
            System.exit(0);
        }
    }
}

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.investbook.BrowserHomePageOpener;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static ru.investbook.loadingpage.PropertiesGetter.getBooleanProperty;

@ExtendWith(MockitoExtension.class)
class LoadingPageHttpServerTest {

    private LoadingPageHttpServer loadingPageHttpServer;

    @BeforeEach
    void setup() {
        loadingPageHttpServer = new LoadingPageHttpServerImpl();
    }

    @Test
    void testBrowserOpensOnLoadingPageStart() {
        try (MockedStatic<BrowserHomePageOpener> browserOpenerMock = Mockito.mockStatic(BrowserHomePageOpener.class);
             MockedStatic<PropertiesGetter> properties = Mockito.mockStatic(PropertiesGetter.class)) {

            String host = "localhost";
            int port = 20300;
            String expectedUrl = "http://" + host + ":" + port;
            properties.when(() -> PropertiesGetter.getProperty(eq("server.address"), any(String.class)))
                    .thenReturn(host);
            properties.when(() -> PropertiesGetter.getIntProperty(eq("server.port"), anyInt()))
                    .thenReturn(port);
            properties.when(() -> getBooleanProperty(eq("investbook.open-home-page-after-start"), anyBoolean()))
                            .thenReturn(true);

            loadingPageHttpServer.start();

            browserOpenerMock.verify(() -> BrowserHomePageOpener.open(expectedUrl), times(1));
        }
    }
}
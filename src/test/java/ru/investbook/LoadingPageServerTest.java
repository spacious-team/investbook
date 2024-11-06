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

package ru.investbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.investbook.loadingpage.LoadingPageServer;

import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class LoadingPageServerTest {

    private LoadingPageServer loadingPageServer;

    @BeforeEach
    void setup() {
        loadingPageServer = new LoadingPageServer();
    }

    @Test
    void testBrowserOpensOnLoadingPageStart() {
        try (MockedStatic<BrowserHomePageOpener> browserOpenerMock = Mockito.mockStatic(BrowserHomePageOpener.class)) {
            loadingPageServer.start();

            String expectedUrl = "http://localhost:" + LoadingPageServer.SERVER_PORT + "/loading";
            browserOpenerMock.verify(() -> BrowserHomePageOpener.open(expectedUrl), times(1));
        }
    }
}
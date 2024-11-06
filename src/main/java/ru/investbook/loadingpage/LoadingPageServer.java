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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import ru.investbook.BrowserHomePageOpener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

@Slf4j
public class LoadingPageServer implements AutoCloseable{
    public static final int SERVER_PORT = 2031;
    public static final int DEFAULT_CLOSE_DELAY_SEC = 120;

    private volatile @Nullable HttpServer server = null;

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
            server.createContext("/", new LoadingPageHandler());
            server.createContext("/main-app-port", new PortHandler());
            server.start();
            this.server = server;
            String loadingPageUrl = "http://localhost:" + SERVER_PORT + "/loading";
            BrowserHomePageOpener.open(loadingPageUrl);
        } catch (IOException e) {
            log.warn("Can't open /loading page", e);
        }
    }

    @Override
    public void close() {
        if (nonNull(server)) {
            //noinspection DataFlowIssue
            server.stop(DEFAULT_CLOSE_DELAY_SEC);
            server = null;
        }
    }

    static class LoadingPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] data;
            try (InputStream in = requireNonNull(getClass().getResourceAsStream("/templates/loading.html"))) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                in.transferTo(out);
                data = out.toByteArray();
            }

            exchange.sendResponseHeaders(200, data.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }

    static class PortHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String mainServerPort = String.valueOf(LoadingPageServerUtils.getMainAppPort());

            exchange.sendResponseHeaders(200, mainServerPort.length());

            try (OutputStream os = exchange.getResponseBody()) {
                byte[] data = mainServerPort.getBytes(UTF_8);
                os.write(data);
            }
        }
    }
}

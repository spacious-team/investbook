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
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.*;
import static ru.investbook.loadingpage.PropertiesGetter.*;

@Slf4j
public class LoadingPageHttpServerImpl implements LoadingPageHttpServer {
    private volatile @Nullable HttpServer server;

    @Override
    public void start() {
        try {
            if (isNull(server)) {
                int port = getIntProperty("server.port", 2030);
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0); // start on any address
                server.createContext("/", new LoadingPageHandler());
                server.start();
                this.server = server;
                log.info("Loading page http server is started on port {}", port);
                if (getBooleanProperty("investbook.open-home-page-after-start", true)) {
                    String address = getProperty("server.address", "localhost");
                    String loadingPageUrl = "http://" + address + ":" + port;
                    BrowserHomePageOpener.open(loadingPageUrl);
                }
            }
        } catch (IOException e) {
            log.warn("Can't open /loading page", e);
        }
    }

    @Override
    @SuppressWarnings({"dereference.of.nullable", "DataFlowIssue"})
    public void close() {
        if (nonNull(server)) {
            InetSocketAddress address = server.getAddress();
            server.stop(0);
            server = null;
            log.info("Loading page http server is stopped on {}", address);
        }
    }


    static class LoadingPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (Objects.equals(exchange.getRequestURI().getPath(), "/")) {
                sendLoadingPage(exchange);
            } else {
                sendNotFound(exchange);  // is required for /templates/loading.html
            }
        }

        private void sendLoadingPage(HttpExchange exchange) throws IOException {
            byte[] data;
            try (InputStream in = requireNonNull(getClass().getResourceAsStream("/templates/loading.html"))) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                in.transferTo(out);
                String page = out.toString(UTF_8);
                page = setServerPortVariable(page);
                data = page.getBytes(UTF_8);

            }

            exchange.sendResponseHeaders(200, data.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }

        private static void sendNotFound(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(404, -1);
        }

        private static String setServerPortVariable(String page) {
            int serverPort = getIntProperty("server.port", 2030);
            return page.replace("{{ server.port }}", String.valueOf(serverPort));
        }
    }
}


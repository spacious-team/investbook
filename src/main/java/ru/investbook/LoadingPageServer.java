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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.*;

@Slf4j
public class LoadingPageServer {
    public static final int SERVER_PORT = 2031;

    private HttpServer server;

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
            server.createContext("/", new LoadingPageHandler());
            server.createContext("/port", new PortHandler());

            server.start();

            String loadingPageUrl = "http://localhost:" + SERVER_PORT + "/loading";
            BrowserHomePageOpener.open(loadingPageUrl);
        } catch (IOException e) {
            log.warn("Loading page warn: {}", e.toString());
        }
    }

    public void stopAfter(int delayInSec) {
        if (server != null) {
            server.stop(delayInSec);
            server = null;
        }
    }

    public boolean isRunning() {
        return server != null;
    }

    static class LoadingPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Path loadingPage = Path.of("src/main/resources/templates/loading.html");
            String response = Files.readString(loadingPage, UTF_8);

            exchange.sendResponseHeaders(200, response.getBytes(UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                byte[] data = response.getBytes(UTF_8);
                os.write(data);
            }
        }
    }

    static class PortHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Properties properties = new Properties();
            String mainServerPort;
            try (InputStream input = new FileInputStream("src/main/resources/application-core.properties")) {
                properties.load(input);
                mainServerPort = properties.getProperty("server.port", "8080");
            } catch (IOException e) {
                mainServerPort = "2030";
                log.warn("Failed to load properties file: {}", e.getMessage());
            }

            exchange.sendResponseHeaders(200, mainServerPort.length());

            try (OutputStream os = exchange.getResponseBody()) {
                byte[] data = mainServerPort.getBytes(UTF_8);
                os.write(data);
            }
        }
    }
}

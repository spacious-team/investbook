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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class LoadingPageServer {
    public static final int SERVER_PORT = 2031;

    private HttpServer server;


    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
        server.createContext("/", new LoadingPageHandler());
        server.setExecutor(null);
        server.start();
        BrowserHomePageOpener.open("http://localhost:%d/loading".formatted(SERVER_PORT));
    }

    public void stopAfter(int delayInSec) {
        if (server != null) {
            server.stop(delayInSec);
        }
    }

    static class LoadingPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = Files.readString(Path.of("src/main/resources/templates/loading.html"), StandardCharsets.ISO_8859_1);
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.ISO_8859_1));
            os.close();
        }
    }

}

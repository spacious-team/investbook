/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.openformat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.openformat.v1_0_0.PortfolioOpenFormatService;
import ru.investbook.openformat.v1_0_0.PortfolioOpenFormatV1_0_0;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;

import static ru.investbook.web.HttpAttachResponseHelper.sendErrorPage;
import static ru.investbook.web.HttpAttachResponseHelper.sendSuccessHeader;

@RestController
@RequestMapping("/portfolio-open-format")
@RequiredArgsConstructor
@Slf4j
public class PortfolioOpenFormatRestController {
    private final ObjectMapper objectMapper;
    private final PortfolioOpenFormatService portfolioOpenFormatService;

    @GetMapping
    public PortfolioOpenFormatV1_0_0 get() {
        return getPortfolioObject();
    }

    @GetMapping("download")
    public void getJsonFile(HttpServletResponse response) throws IOException {
        try {
            long t0 = System.nanoTime();
            String fileName = "portfolio.json";
            PortfolioOpenFormatV1_0_0 object = getPortfolioObject();
            sendSuccessHeader(response, fileName, "application/json");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(10240);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, object);
            outputStream.writeTo(response.getOutputStream());
            log.info("Файл '{}' в формате 'Portfolio Open Format' сформирован за {}",
                    fileName, Duration.ofNanos(System.nanoTime() - t0));
        } catch (Exception e) {
            log.error("Ошибка сборки отчета", e);
            sendErrorPage(response, e);
        }
        response.flushBuffer();
    }

    private PortfolioOpenFormatV1_0_0 getPortfolioObject() {
        return portfolioOpenFormatService.generate();
    }
}

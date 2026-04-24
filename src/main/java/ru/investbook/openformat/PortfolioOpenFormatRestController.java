
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

package ru.investbook.openformat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.investbook.openformat.v1_1_0.PortfolioOpenFormatBuilder;
import ru.investbook.openformat.v1_1_0.PortfolioOpenFormatPersister;
import ru.investbook.openformat.v1_1_0.PortfolioOpenFormatV1_1_0;
import ru.investbook.parser.ValidatorService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.Duration;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Portfolio Open Format", description = "Сохранение и отображение портфеля в унифицированном формате")
@RequestMapping("/portfolio-open-format")
public class PortfolioOpenFormatRestController {
    private final ObjectMapper objectMapper;
    private final PortfolioOpenFormatBuilder portfolioOpenFormatFactory;
    private final PortfolioOpenFormatPersister portfolioOpenFormatPersister;
    private final ValidatorService validator;

    @GetMapping("/records")
    @Operation(summary = "Отобразить портфель", operationId = "getPortfolioOpenFormat", responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public PortfolioOpenFormatV1_1_0 get() {
        long t0 = System.nanoTime();
        PortfolioOpenFormatV1_1_0 object = portfolioOpenFormatFactory.create();
        validate(object);
        log.info("Данные в формате 'Portfolio Open Format' подготовлены за {}", Duration.ofNanos(System.nanoTime() - t0));
        return object;
    }

    @PostMapping("/records")
    @Operation(summary = "Сохранить портфель", operationId = "postPortfolioOpenFormat", responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public void post(@RequestParam("file") MultipartFile file) throws URISyntaxException, IOException {
        try (InputStream inputStream = file.getInputStream()) { // creates new input stream
            long t0 = System.nanoTime();
            PortfolioOpenFormatV1_1_0 object = objectMapper.readValue(inputStream, PortfolioOpenFormatV1_1_0.class);
            validate(object);
            portfolioOpenFormatPersister.persist(object);
            log.info("Выполнено восстановление данных 'Portfolio Open Format' за {}", Duration.ofNanos(System.nanoTime() - t0));
        }
    }

    private void validate(PortfolioOpenFormatV1_1_0 object) {
        try {
            validator.validate(object);
        } catch (Exception e) {
            log.warn("Найдены ошибки в данных формата 'Open Portfolio Format'", e);
        }
    }
}

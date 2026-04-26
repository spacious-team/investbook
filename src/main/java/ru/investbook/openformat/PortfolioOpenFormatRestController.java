
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.openformat.v1_1_0.PortfolioOpenFormatBuilder;
import ru.investbook.openformat.v1_1_0.PortfolioOpenFormatPersister;
import ru.investbook.openformat.v1_1_0.PortfolioOpenFormatV1_1_0;

import java.time.Duration;

@Slf4j
// Без аннотации валидация возвращаемых типов @Valid запускается только при REST запросах
// С аннотацией валидация срабатывает и при вызове из других бинов методов текущего бина
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Portfolio Open Format", description = "Data exchange in unified Portfolio Open Format")
@RequestMapping("/portfolio-open-format")
public class PortfolioOpenFormatRestController {
    private final PortfolioOpenFormatBuilder portfolioOpenFormatFactory;
    private final PortfolioOpenFormatPersister portfolioOpenFormatPersister;

    @GetMapping("/records")
    @Operation(summary = "Get portfolio", operationId = "getPortfolioOpenFormat", responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public @Valid PortfolioOpenFormatV1_1_0 get() {
        long t0 = System.nanoTime();
        PortfolioOpenFormatV1_1_0 object = portfolioOpenFormatFactory.create();
        log.info("Данные в формате 'Portfolio Open Format' подготовлены за {}", Duration.ofNanos(System.nanoTime() - t0));
        return object;
    }

    @PostMapping("/records")
    @Operation(summary = "Upload portfolio", operationId = "postPortfolioOpenFormat", responses = {
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "500", content = @Content)})
    public void post(@RequestBody @Valid PortfolioOpenFormatV1_1_0 object) {
        long t0 = System.nanoTime();
        portfolioOpenFormatPersister.persist(object);
        log.info("Выполнено восстановление данных 'Portfolio Open Format' за {}", Duration.ofNanos(System.nanoTime() - t0));
    }
}

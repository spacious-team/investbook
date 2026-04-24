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
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.investbook.openformat.v1_1_0.PortfolioOpenFormatBuilder;
import ru.investbook.openformat.v1_1_0.PortfolioOpenFormatPersister;
import ru.investbook.openformat.v1_1_0.PortfolioOpenFormatV1_1_0;
import ru.investbook.parser.ValidatorService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;

import static ru.investbook.web.HttpAttachResponseHelper.sendErrorHttpHeader;
import static ru.investbook.web.HttpAttachResponseHelper.sendSuccessHeader;
import static ru.investbook.web.ReportControllerHelper.exceptionToString;

@Controller
@RequestMapping("/portfolio-open-format")
@RequiredArgsConstructor
@Slf4j
public class PortfolioOpenFormatController {
    private final ObjectMapper objectMapper;
    private final PortfolioOpenFormatBuilder portfolioOpenFormatFactory;
    private final PortfolioOpenFormatPersister portfolioOpenFormatPersister;
    private final ValidatorService validator;

    /**
     * @return null if Thymeleaf render is not required, attach response is sent already
     */
    @GetMapping("/backup/download")
    public @Nullable String download(HttpServletResponse response, Model model) {
        try {
            long t0 = System.nanoTime();
            String fileName = "portfolio.json";
            PortfolioOpenFormatV1_1_0 object = portfolioOpenFormatFactory.create();
            validate(object);
            sendSuccessHeader(response, fileName, "application/json");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(10240);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, object);
            outputStream.writeTo(response.getOutputStream());
            log.info("Файл '{}' в формате 'Portfolio Open Format' сформирован за {}",
                    fileName, Duration.ofNanos(System.nanoTime() - t0));
            response.flushBuffer();
            return null;  // response is already build
        } catch (Exception e) {
            log.error("Ошибка генерации файла бэкапа", e);
            sendErrorHttpHeader(response);
            model.addAttribute("message", "Ошибка подготовки бекапа");
            model.addAttribute("stackTrace", exceptionToString(e));
            return "stack-trace";
        }
    }

    @PostMapping("/backup/upload")
    public String upload(@RequestParam("file") MultipartFile file, Model model) {
        try (InputStream inputStream = file.getInputStream()) { // creates new input stream
            long t0 = System.nanoTime();
            PortfolioOpenFormatV1_1_0 object = objectMapper.readValue(inputStream, PortfolioOpenFormatV1_1_0.class);
            validate(object);
            portfolioOpenFormatPersister.persist(object);
            log.info("Выполнено восстановление данных из бэкапа за {}", Duration.ofNanos(System.nanoTime() - t0));
            model.addAttribute("message", "Бекап восстановлен");
            return "success";
        } catch (Exception e) {
            log.error("Ошибка восстановления данных из бэкапа", e);
            model.addAttribute("message", "Возможно это не файл в формате \"Open Portfolio Format\"");
            model.addAttribute("stackTrace", exceptionToString(e));
            return "stack-trace";
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

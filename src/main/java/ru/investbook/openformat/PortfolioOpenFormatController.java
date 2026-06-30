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

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.investbook.openformat.v1_1_0.PortfolioOpenFormatV1_1_0;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static ru.investbook.web.HttpAttachResponseHelper.sendErrorHttpHeader;
import static ru.investbook.web.HttpAttachResponseHelper.sendSuccessHeader;
import static ru.investbook.web.ReportControllerHelper.exceptionToString;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/portfolio-open-format")
public class PortfolioOpenFormatController {
    private final PortfolioOpenFormatRestController portfolioOpenFormatRestController;
    private final ObjectMapper objectMapper;

    /**
     * @return null if Thymeleaf render is not required, attach response has been already sent
     */
    @GetMapping("/backup/download")
    public @Nullable String download(HttpServletResponse response, Model model) {
        try {
            String fileName = "portfolio.json";
            PortfolioOpenFormatV1_1_0 object = portfolioOpenFormatRestController.get();
            sendSuccessHeader(response, fileName, "application/json");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(10240);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, object);
            outputStream.writeTo(response.getOutputStream());
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
    public String upload(@RequestPart("file") @Valid PortfolioOpenFormatV1_1_0 object, Model model) {
        try {
            portfolioOpenFormatRestController.post(object);
            model.addAttribute("message", "Бекап восстановлен");
            return "success";
        } catch (Exception e) {
            log.error("Ошибка восстановления данных из бэкапа", e);
            model.addAttribute("message", "Возможно это не файл в формате \"Open Portfolio Format\"");
            model.addAttribute("stackTrace", exceptionToString(e));
            return "stack-trace";
        }
    }

    /**
     * Обработает ошибки валидации ({@link @Valid}) для методов текущего контроллера
     */
    @SuppressWarnings("unused")
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationExceptions(MethodArgumentNotValidException e, Model model) {
        log.error("Ошибка восстановления данных из бэкапа", e);
        model.addAttribute("message",
                "Возможно это не файл в формате \"Open Portfolio Format\": " + e.getMessage());
        return "stack-trace";
    }
}

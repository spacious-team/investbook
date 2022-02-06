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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.investbook.openformat.v1_0_0.PortfolioOpenFormatBuilder;
import ru.investbook.openformat.v1_0_0.PortfolioOpenFormatPersister;
import ru.investbook.openformat.v1_0_0.PortfolioOpenFormatV1_0_0;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;

import static ru.investbook.web.HttpAttachResponseHelper.sendErrorPage;
import static ru.investbook.web.HttpAttachResponseHelper.sendSuccessHeader;
import static ru.investbook.web.ReportControllerHelper.errorPage;

@RestController
@RequestMapping("/portfolio-open-format")
@RequiredArgsConstructor
@Slf4j
public class PortfolioOpenFormatRestController {
    private final ObjectMapper objectMapper;
    private final PortfolioOpenFormatBuilder portfolioOpenFormatFactory;
    private final PortfolioOpenFormatPersister portfolioOpenFormatPersister;

    @GetMapping("download")
    public void download(HttpServletResponse response) throws IOException {
        try {
            long t0 = System.nanoTime();
            String fileName = "portfolio.json";
            PortfolioOpenFormatV1_0_0 object = portfolioOpenFormatFactory.create();
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

    @PostMapping("upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) { // creates new input stream
            PortfolioOpenFormatV1_0_0 object = objectMapper.readValue(inputStream, PortfolioOpenFormatV1_0_0.class);
            portfolioOpenFormatPersister.persist(object);
            return ok();
        } catch (Exception e) {
            return errorPage("Возможно это не файл в формате \"Open Portfolio Format\"", Collections.emptyList());
        }
    }

    private ResponseEntity<String> ok() {
        return ResponseEntity.ok("""
                Файл загружен <a href="/">[ok]</a>
                <script type="text/javascript">document.location.href="/"</script>
                """);
    }
}

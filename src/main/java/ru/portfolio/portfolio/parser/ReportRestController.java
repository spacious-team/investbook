/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.portfolio.portfolio.parser;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.portfolio.portfolio.parser.psb.PsbBrokerReport;
import ru.portfolio.portfolio.parser.psb.PsbReportTableFactory;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportRestController {
    private final ReportParserService reportParserService;
    private FileSystem jimfs = Jimfs.newFileSystem(Configuration.unix());

    @PostMapping("/a")
    public String a() {
        throw new IllegalArgumentException();
    }

    @PostMapping("/reports")
    public String post(@RequestParam("reports") MultipartFile[] reports,
                       @RequestParam(name = "format", required = false) String format) {
        if (format == null || format.isEmpty()) {
            format = "psb";
        }
        format = format.toLowerCase();
        List<Exception> exceptions = new ArrayList<>();
        for (MultipartFile report : reports) {
            try {
                if (report == null || report.isEmpty()) {
                    continue;
                }
                long t0 = System.nanoTime();
                byte[] bytes = report.getBytes();
                String originalFilename = report.getOriginalFilename();
                Path path = jimfs.getPath(originalFilename != null ? originalFilename : UUID.randomUUID().toString());
                Files.write(path, bytes);
                if ("psb".equals(format)) {
                    parsePsbReport(path);
                } else {
                    throw new IllegalArgumentException("Неизвестный формат " + format);
                }
                log.info("Загрузка отчета {} завершена за {}", path.getFileName(), Duration.ofNanos(System.nanoTime() - t0));
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (exceptions.isEmpty()) {
            return "ok";
        } else {
            throw new RuntimeException(exceptions.stream().map(Throwable::getMessage).collect(Collectors.joining(", ")));
        }
    }

    private void parsePsbReport(Path path) {
        try (PsbBrokerReport brockerReport = new PsbBrokerReport(path)) {
            ReportTableFactory reportTableFactory = new PsbReportTableFactory(brockerReport);
            reportParserService.parse(reportTableFactory);
        } catch (Exception e) {
            log.warn("Не могу открыть/закрыть отчет {}", path, e);
            throw new RuntimeException(e);
        }
    }
}

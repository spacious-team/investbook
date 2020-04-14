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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.portfolio.portfolio.parser.psb.PsbBrokerReport;
import ru.portfolio.portfolio.parser.psb.PsbReportTableFactory;
import ru.portfolio.portfolio.parser.uralsib.UralsibBrokerReport;
import ru.portfolio.portfolio.parser.uralsib.UralsibReportTableFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportRestController {
    private static final Path reportBackupPath = Paths.get(
            System.getProperty("user.home", ""),
            "portfolio-report-backups");
    private final ReportParserService reportParserService;

    @PostMapping("/reports")
    public String post(@RequestParam("reports") MultipartFile[] reports,
                       @RequestParam(name = "format", required = false) String format) {
        if (format == null || format.isEmpty()) {
            format = "psb";
        }
        BrockerType brocker =BrockerType.valueOf(format.toUpperCase());
        List<Exception> exceptions = new ArrayList<>();
        for (MultipartFile report : reports) {
            try {
                if (report == null || report.isEmpty()) {
                    continue;
                }
                long t0 = System.nanoTime();
                Path path = saveToBackup(brocker, report);
                String originalFileName = report.getOriginalFilename();
                switch (brocker) {
                    case PSB:
                        parsePsbReport(report);
                        break;
                    case URALSIB:
                        if (originalFileName != null && !originalFileName.contains("_invest_")) {
                            log.warn("Рекомендуется загружать отчеты содержащие в имени файла слово 'invest'");
                        }
                        parseUralsibReport(report);
                        break;
                        default:
                        throw new IllegalArgumentException("Неизвестный формат " + format);
                }
                log.info("Загрузка отчета {} завершена за {}, бекап отчета сохранен в {}", report.getOriginalFilename(),
                        Duration.ofNanos(System.nanoTime() - t0), path.toAbsolutePath());
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

    /**
     * @return backup file
     */
    private Path saveToBackup(BrockerType brocker, MultipartFile report) throws IOException {
        byte[] bytes = report.getBytes();
        String originalFilename = report.getOriginalFilename();
        Path backupPath = reportBackupPath.resolve(brocker.name().toLowerCase());
        Files.createDirectories(backupPath);
        Path path = backupPath.resolve((originalFilename != null) ?
                originalFilename :
                UUID.randomUUID().toString());
        for (int i = 1; i < 1e6; i++) {
            if (!Files.exists(path)) {
                break;
            }
            path = backupPath.resolve((originalFilename != null) ?
                    "Копия " + i + " - " + (originalFilename) :
                    UUID.randomUUID().toString());
        }
        Files.write(path, bytes);
        return path;
    }

    private void parsePsbReport(MultipartFile report) {
        try (PsbBrokerReport brockerReport = new PsbBrokerReport(report.getOriginalFilename(), report.getInputStream())) {
            ReportTableFactory reportTableFactory = new PsbReportTableFactory(brockerReport);
            reportParserService.parse(reportTableFactory);
        } catch (Exception e) {
            log.warn("Не могу открыть/закрыть отчет {}", report.getOriginalFilename(), e);
            throw new RuntimeException(e);
        }
    }

    private void parseUralsibReport(MultipartFile report) {
        try (ZipInputStream zis = new ZipInputStream(report.getInputStream())) {
            try (UralsibBrokerReport brockerReport = new UralsibBrokerReport(zis)) {
                ReportTableFactory reportTableFactory = new UralsibReportTableFactory(brockerReport);
                reportParserService.parse(reportTableFactory);
            } catch (Exception e) {
                log.warn("Не могу открыть/закрыть отчет {}", report.getOriginalFilename(), e);
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            log.warn("Не могу открыть zip архив {}", report.getOriginalFilename(), e);
            throw new RuntimeException(e);
        }
    }
}

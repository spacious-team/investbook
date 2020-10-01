/*
 * InvestBook
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

package ru.investbook.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.investbook.PortfolioProperties;
import ru.investbook.view.ForeignExchangeRateService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportRestController {
    private final ReportParserService reportParserService;
    private final ForeignExchangeRateService foreignExchangeRateService;
    private final PortfolioProperties portfolioProperties;
    private final Collection<BrokerReportFactory> brokerReportFactories;
    private final Collection<ReportTablesFactory> reportTablesFactories;

    @PostMapping("/reports")
    public ResponseEntity<String> post(@RequestParam("reports") MultipartFile[] reports,
                                       @RequestParam(name = "format", required = false) String format) {
        if (format == null || format.isEmpty()) {
            format = "psb";
        }
        BrokerType broker = BrokerType.valueOf(format.toUpperCase());
        List<Exception> exceptions = new ArrayList<>();
        for (MultipartFile report : reports) {
            try {
                if (report == null || report.isEmpty()) {
                    continue;
                }
                long t0 = System.nanoTime();
                Path path = saveToBackup(broker, report);
                String originalFileName = report.getOriginalFilename();
                switch (broker) {
                    case URALSIB:
                        if (originalFileName != null && !originalFileName.contains("_invest_")) {
                            log.warn("Рекомендуется загружать отчеты, содержащие в имени файла слово 'invest'");
                        }
                    case PSB:
                    case VTB:
                        parseReport(report);
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
            return ResponseEntity.ok("Отчеты загружены <a href=\"/\">[ok]</a>");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(exceptions.stream()
                            .map(e -> {
                                StringWriter sw = new StringWriter();
                                PrintWriter pw = new PrintWriter(sw);
                                e.printStackTrace(pw);
                                return sw.toString().replace("\n", "</br>");
                            }).collect(Collectors.joining("</br></br> - ",
                                    "<b>Ошибка загрузки отчетов</b> <a href=\"/\">[назад]</a><br/>" +
                                    "<span style=\"font-size: smaller; color: gray;\">Вы можете " +
                                    "<a href=\"https://github.com/spacious-team/investbook/issues\">сообщить</a> об ошибке " +
                                    "разработчикам</span></br></br> - ",
                                    "")));
        }
    }

    /**
     * @return backup file
     */
    private Path saveToBackup(BrokerType broker, MultipartFile report) throws IOException {
        byte[] bytes = report.getBytes();
        String originalFilename = report.getOriginalFilename();
        Path backupPath = portfolioProperties.getReportBackupPath().resolve(broker.name().toLowerCase());
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

    private void parseReport(MultipartFile report) {
        try (BrokerReport brokerReport = getBrokerReport(report)) {
            ReportTables reportTables = getReportTables(brokerReport);
            reportParserService.parse(reportTables);
        } catch (Exception e) {
            String error = "Произошла ошибка парсинга отчета " + report.getOriginalFilename();
            log.warn(error, e);
            throw new RuntimeException(error, e);
        }
    }

    private BrokerReport getBrokerReport(MultipartFile report) throws IOException {
        BrokerReport brokerReport = null;
        // converting to input stream supporting mark
        ByteArrayInputStream is = castToBayteArrayInputStream(report.getInputStream());
        for (BrokerReportFactory brokerReportFactory : brokerReportFactories) {
            brokerReport = brokerReportFactory.create(report.getOriginalFilename(), is);
            if (brokerReport != null) {
                break;
            }
        }
        if (brokerReport == null) {
            throw new IllegalArgumentException("Неизвестный формат отчета " + report.getOriginalFilename());
        }
        return brokerReport;
    }

    private static ByteArrayInputStream castToBayteArrayInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        inputStream.transferTo(out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    private ReportTables getReportTables(BrokerReport brokerReport) {
        for (ReportTablesFactory reportTablesFactory : reportTablesFactories) {
            if (reportTablesFactory.canCreate(brokerReport)) {
                return reportTablesFactory.create(brokerReport);
            }
        }
        throw new IllegalArgumentException(
                "Can't fina ReportTablesFactory for broker report of type: " + brokerReport.getClass().getSimpleName());
    }
}

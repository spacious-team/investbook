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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.investbook.PortfolioProperties;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reports")
@Slf4j
public class ReportRestController {
    private final ReportParserService reportParserService;
    private final PortfolioProperties portfolioProperties;
    private final Collection<BrokerReportFactory> brokerReportFactories;
    private final Collection<ReportTablesFactory> reportTablesFactories;


    @GetMapping
    public String getReports(Model model) {
        Collection<String> brokerNames = brokerReportFactories.stream()
                .map(BrokerReportFactory::getBrokerName)
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
        model.addAttribute("brokerNames", brokerNames);
        return "reports";
    }

    @PostMapping
    public ResponseEntity<String> post(@RequestParam("reports") MultipartFile[] reports,
                                       @RequestParam(name = "broker", required = false) String broker) {
        List<Exception> exceptions = new ArrayList<>();
        for (MultipartFile report : reports) {
            try {
                if (report != null && !report.isEmpty()) {
                    long t0 = System.nanoTime();
                    String brokerName = parseReport(report, broker);
                    Path path = saveToBackup(brokerName, report);
                    log.info("Загрузка отчета {} завершена за {}, бекап отчета сохранен в {}", report.getOriginalFilename(),
                            Duration.ofNanos(System.nanoTime() - t0), path.toAbsolutePath());
                }
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
    private Path saveToBackup(String brokerName, MultipartFile report) throws IOException {
        Objects.requireNonNull(brokerName, "Наименование брокера, предоставившего отчет, не определено");
        byte[] bytes = report.getBytes();
        String originalFilename = report.getOriginalFilename();
        Path backupPath = portfolioProperties.getReportBackupPath().resolve(brokerName);
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

    /**
     * @param report           report
     * @param providedByBroker broker what generates report, may be null if unknown
     * @return the exact name of the broker providing the report
     * @throws RuntimeException if report has broken format or parser not found
     */
    private String parseReport(MultipartFile report, String providedByBroker) {
        try (BrokerNameAndReport brokerNameAndReport = getBrokerReport(report, providedByBroker)) {
            ReportTables reportTables = getReportTables(brokerNameAndReport.getBrokerReport());
            reportParserService.parse(reportTables);
            return brokerNameAndReport.getBrokerName();
        } catch (Exception e) {
            String error = "Произошла ошибка парсинга отчета " + report.getOriginalFilename();
            log.warn(error, e);
            throw new RuntimeException(error, e);
        }
    }

    private BrokerNameAndReport getBrokerReport(MultipartFile report, String providedByBroker) throws IOException {
        if (StringUtils.isEmpty(providedByBroker)) {
            return getReportOfUnknownBroker(report);
        } else {
            return getReportOfKnownBroker(report, providedByBroker);
        }
    }

    private BrokerNameAndReport getReportOfUnknownBroker(MultipartFile report) throws IOException {
        BrokerReport brokerReport = null;
        // convert to mark supporting input stream
        ByteArrayInputStream is = castToBayteArrayInputStream(report.getInputStream());
        for (BrokerReportFactory brokerReportFactory : brokerReportFactories) {
            brokerReport = brokerReportFactory.create(report.getOriginalFilename(), is);
            if (brokerReport != null) {
                return new BrokerNameAndReport(brokerReportFactory.getBrokerName(), brokerReport);
            }
        }
        throw new IllegalArgumentException("Неизвестный формат отчета " + report.getOriginalFilename());
    }

    private BrokerNameAndReport getReportOfKnownBroker(MultipartFile report, String providedByBroker) throws IOException {
        ByteArrayInputStream is = castToBayteArrayInputStream(report.getInputStream());
        return findBrokerReportFactory(providedByBroker).stream()
                .map(f -> {
                    BrokerReport brokerReport = f.create(report.getOriginalFilename(), is);
                    if (brokerReport != null) {
                        return new BrokerNameAndReport(f.getBrokerName(), brokerReport);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Файл " + report.getOriginalFilename() +
                        " не является отчетом брокера " + providedByBroker));
    }

    private Collection<BrokerReportFactory> findBrokerReportFactory(String broker) {
        return brokerReportFactories.stream()
                .filter(b -> b.getBrokerName().equalsIgnoreCase(broker))
                .collect(Collectors.toList());
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

    @Getter
    @RequiredArgsConstructor
    private static class BrokerNameAndReport implements AutoCloseable {
        private final String brokerName;
        private final BrokerReport brokerReport;

        @Override
        public void close() throws Exception {
            brokerReport.close();
        }
    }
}
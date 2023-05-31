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

package ru.investbook.parser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.BrokerReportFactory;
import org.spacious_team.broker.report_parser.api.ReportTables;
import org.spacious_team.broker.report_parser.api.ReportTablesFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.investbook.InvestbookProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerReportParserServiceImpl implements BrokerReportParserService {

    private final InvestbookProperties investbookProperties;
    private final ReportParserService reportParserService;
    private final List<BrokerReportFactory> brokerReportFactories;
    private final Collection<ReportTablesFactory> reportTablesFactories;


    @SneakyThrows
    @Override
    public void parseReport(InputStream inputStream, String fileName, String broker) {
        try (ByteArrayInputStream is = castToByteArrayInputStream(inputStream)) {
            long t0 = System.nanoTime();
            is.mark(Integer.MAX_VALUE);
            String brokerName = parseReport0(is, fileName, broker);
            if (investbookProperties.isReportBackup()) {
                is.reset();
                Path path = saveToBackup(is, fileName, brokerName);
                log.info("Загрузка отчета {} завершена за {}, бекап отчета сохранен в {}",
                        fileName, Duration.ofNanos(System.nanoTime() - t0), path.toAbsolutePath());
            } else {
                log.info("Загрузка отчета {} завершена за {}, бекап отключен конфигурацией",
                        fileName, Duration.ofNanos(System.nanoTime() - t0));
            }
        }
    }

    public static ByteArrayInputStream castToByteArrayInputStream(InputStream inputStream) throws IOException {
        if (inputStream instanceof ByteArrayInputStream) {
            return (ByteArrayInputStream) inputStream;
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            inputStream.transferTo(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    /**
     * @param inputStream      file content
     * @param fileName         file name
     * @param providedByBroker broker what generates report, may be null if unknown
     * @return the exact name of the broker providing the report
     * @throws RuntimeException if report has broken format or parser not found
     */
    private String parseReport0(ByteArrayInputStream inputStream, String fileName, String providedByBroker) {
        try (BrokerNameAndReport brokerNameAndReport = getBrokerReport(inputStream, fileName, providedByBroker)) {
            ReportTables reportTables = getReportTables(brokerNameAndReport.getBrokerReport());
            reportParserService.parse(reportTables);
            return brokerNameAndReport.getBrokerName();
        } catch (Exception e) {
            String error = "Произошла ошибка парсинга отчета '" + fileName + "'";
            log.warn(error, e);
            throw new RuntimeException(error, e);
        }
    }

    /**
     * @return backup file
     */
    @SneakyThrows
    private Path saveToBackup(InputStream inputStream, String fileName, String brokerName) {
        Objects.requireNonNull(brokerName, "Наименование брокера, предоставившего отчет, не определено");
        Path backupPath = investbookProperties.getReportBackupPath().resolve(brokerName);
        Files.createDirectories(backupPath);
        Path path = backupPath.resolve((fileName != null) ?
                fileName :
                UUID.randomUUID().toString());
        for (int i = 1; i < 1e6; i++) {
            if (!Files.exists(path)) {
                break;
            }
            path = backupPath.resolve((fileName != null) ?
                    "Копия " + i + " - " + (fileName) :
                    UUID.randomUUID().toString());
        }
        Files.copy(inputStream, path);
        return path;
    }

    private BrokerNameAndReport getBrokerReport(ByteArrayInputStream inputStream, String fileName, String providedByBroker) {
        if (StringUtils.hasLength(providedByBroker)) {
            return getReportOfKnownBroker(inputStream, fileName, providedByBroker);
        } else {
            return getReportOfUnknownBroker(inputStream, fileName);
        }
    }

    @SneakyThrows
    private BrokerNameAndReport getReportOfUnknownBroker(ByteArrayInputStream inputStream, String fileName) {
        Optional<BrokerReport> brokerReport;
        // convert to mark supporting input stream

        for (BrokerReportFactory brokerReportFactory : brokerReportFactories) {
            if (!brokerReportFactory.canCreate(fileName, inputStream)) continue;
            brokerReport = brokerReportFactory.create(fileName, inputStream);
            if (brokerReport.isPresent()) {
                return new BrokerNameAndReport(brokerReportFactory.getBrokerName(), brokerReport.get());
            }
        }
        throw new IllegalArgumentException("Неизвестный формат отчета '" + fileName + "'");
    }

    @SneakyThrows
    private BrokerNameAndReport getReportOfKnownBroker(ByteArrayInputStream inputStream, String fileName, String providedByBroker) {
        return findBrokerReportFactory(providedByBroker).stream()
                .flatMap(f -> f.create(fileName, inputStream)
                        .map(report -> new BrokerNameAndReport(f.getBrokerName(), report))
                        .stream())
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Файл " + fileName +
                        " не является отчетом брокера " + providedByBroker));
    }

    private Collection<BrokerReportFactory> findBrokerReportFactory(String broker) {
        return brokerReportFactories.stream()
                .filter(b -> b.getBrokerName().equalsIgnoreCase(broker))
                .collect(Collectors.toList());
    }

    private ReportTables getReportTables(BrokerReport brokerReport) {
        for (ReportTablesFactory reportTablesFactory : reportTablesFactories) {
            if (reportTablesFactory.canCreate(brokerReport)) {
                ReportTables reportTables = reportTablesFactory.create(brokerReport);
                return new ReportTablesCachingWrapper(reportTables);
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

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

package ru.investbook.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.investbook.entity.StockMarketIndexEntity;
import ru.investbook.repository.StockMarketIndexRepository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

@Service
@Slf4j
@RequiredArgsConstructor
public class Sp500Service {
    // Link has been copied from https://www.spglobal.com -> menu -> indices -> S&P 500 -> 10 Years
    private final URI uri = fromUriString("https://www.spglobal.com/spdji/en/idsexport/file.xls")
            .queryParam("hostIdentifier", UUID.randomUUID())
            .queryParam("redesignExport", true)
            .queryParam("languageId", 1)
            .queryParam("selectedModule", "PerformanceGraphView")
            .queryParam("selectedSubModule", "Graph")
            .queryParam("yearFlag", "tenYearFlag")
            .queryParam("indexId", 340)
            .build()
            .toUri();
    private final StockMarketIndexRepository stockMarketIndexRepository;
    private final RestTemplate restTemplate;

    @Transactional
    public void update() {
        try {
            long t0 = System.nanoTime();
            ResponseEntity<Resource> response = downloadSp500Data();
            InputStream is = getInputStream(response);
            updateBy(is);
            log.info("Индекс S&P 500 обновлен за {}", Duration.ofNanos(System.nanoTime() - t0));
        } catch (Exception e) {
            throw new RuntimeException("Не смог обновить значения индекса S&P 500", e);
        }
    }

    private ResponseEntity<Resource> downloadSp500Data() {
        // Http server without headers returns 403 Forbidden
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:139.0) Gecko/20100101 Firefox/139.0");
        headers.add(HttpHeaders.ACCEPT_ENCODING, "gzip");
        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        return restTemplate.exchange(uri, GET, httpEntity, Resource.class);
    }

    private InputStream getInputStream(ResponseEntity<Resource> response) throws IOException {
        @Nullable Resource resource = response.getBody();
        InputStream is = requireNonNull(resource, () -> "Не удалось скачать S&P 500 с адреса " + uri)
                .getInputStream();
        @Nullable List<String> contentEncoding = response.getHeaders()
                .get("Content-Encoding");
        if (nonNull(contentEncoding) && contentEncoding.contains("gzip")) {
            is = new GZIPInputStream(is);
        }
        return is;
    }

    private void updateBy(InputStream inputStream) throws IOException {
        Workbook book = new HSSFWorkbook(inputStream);
        new ExcelSheet(book.getSheetAt(0))
                .createNameless("Effective date", TableHeader.class)
                .stream()
                .filter(Objects::nonNull)
                .map(Sp500Service::getIndexValue)
                .forEach(this::save);
    }

    private static StockMarketIndexEntity getIndexValue(TableRow row) {
        return StockMarketIndexEntity.builder()
                .date(row.getLocalDateTimeCellValue(TableHeader.DATE).toLocalDate())
                .sp500(row.getBigDecimalCellValue(TableHeader.VALUE))
                .build();
    }

    private void save(StockMarketIndexEntity entity) {
        try {
            stockMarketIndexRepository.save(entity);
        } catch (Exception e) {
            log.debug("Ошибка сохранения {}, может быть запись уже существует?", entity);
        }
    }

    @RequiredArgsConstructor
    private enum TableHeader implements TableHeaderColumn {
        DATE(PatternTableColumn.of("Effective date")),
        VALUE(PatternTableColumn.of("S&P 500"));

        @Getter
        private final TableColumn column;
    }
}

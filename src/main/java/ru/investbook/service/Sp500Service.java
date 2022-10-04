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
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.investbook.entity.StockMarketIndexEntity;
import ru.investbook.repository.StockMarketIndexRepository;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;

import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

@Service
@Slf4j
@RequiredArgsConstructor
public class Sp500Service {
    private final URI uri = fromHttpUrl("https://www.spglobal.com/spdji/en/idsexport/file.xls")
            .queryParam("redesignExport", true)
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
            Resource resource = restTemplate.getForObject(uri, Resource.class);
            updateBy(resource);
            log.info("Индекс S&P 500 обновлен за {}", Duration.ofNanos(System.nanoTime() - t0));
        } catch (Exception e) {
            throw new RuntimeException("Не смог обновить значения индекса S&P 500", e);
        }
    }

    private void updateBy(Resource resource) throws IOException {
        Objects.requireNonNull(resource, () -> "Не удалось скачать S&P 500 с адреса " + uri);
        Workbook book = new HSSFWorkbook(resource.getInputStream());
        new ExcelSheet(book.getSheetAt(0))
                .createNameless("Effective date", TableHeader.class)
                .stream()
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
    private enum TableHeader implements TableColumnDescription {
        DATE(TableColumnImpl.of("Effective date")),
        VALUE(TableColumnImpl.of("S&P 500"));

        @Getter
        private final TableColumn column;
    }
}

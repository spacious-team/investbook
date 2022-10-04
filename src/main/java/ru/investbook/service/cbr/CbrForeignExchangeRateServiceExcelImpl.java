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

package ru.investbook.service.cbr;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestTemplate;
import ru.investbook.converter.ForeignExchangeRateConverter;
import ru.investbook.repository.ForeignExchangeRateRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

public class CbrForeignExchangeRateServiceExcelImpl extends AbstractCbrForeignExchangeRateService {
    private static final String uri = "https://www.cbr.ru/Queries/UniDbQuery/DownloadExcel/98956?" +
            "VAL_NM_RQ={currency}&" +
            "FromDate={from-date}&" +
            "ToDate={to-date}&" +
            "mode=1&" +
            "Posted=true";
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final RestTemplate restTemplate;

    public CbrForeignExchangeRateServiceExcelImpl(ForeignExchangeRateRepository foreignExchangeRateRepository,
                                                  ForeignExchangeRateConverter foreignExchangeRateConverter,
                                                  RestTemplate restTemplate) {
        super(foreignExchangeRateRepository, foreignExchangeRateConverter);
        this.restTemplate = restTemplate;
    }

    @SneakyThrows
    @Override
    protected void updateCurrencyRate(String currencyPair, String currencyId, LocalDate fromDate) {
        Resource resource = restTemplate.getForObject(
                uri,
                Resource.class,
                Map.of("currency", currencyId,
                        "from-date", fromDate.format(dateFormatter),
                        "to-date", LocalDate.now().format(dateFormatter)));
        updateBy(resource, currencyPair);
    }

    private void updateBy(Resource resource, String currencyPair) throws IOException {
        Objects.requireNonNull(resource, () -> "Не удалось скачать курсы валют");
        Workbook book = new XSSFWorkbook(resource.getInputStream());
        new ExcelSheet(book.getSheetAt(0))
                .createNameless("data", TableHeader.class)
                .stream()
                .map(row -> getRate(row, currencyPair))
                .forEach(this::save);
    }

    private static ForeignExchangeRate getRate(TableRow row, String currencyPair) {
        return ForeignExchangeRate.builder()
                .date(row.getLocalDateTimeCellValue(TableHeader.DATE).toLocalDate())
                .currencyPair(currencyPair)
                .rate(row.getBigDecimalCellValue(TableHeader.FX_RATE))
                .build();
    }

    @RequiredArgsConstructor
    public enum TableHeader implements TableColumnDescription {
        DATE(TableColumnImpl.of("data")),
        FX_RATE(TableColumnImpl.of("curs"));

        @Getter
        private final TableColumn column;
    }
}

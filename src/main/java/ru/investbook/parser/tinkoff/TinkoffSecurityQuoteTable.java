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

package ru.investbook.parser.tinkoff;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import org.springframework.util.StringUtils;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.report.ForeignExchangeRateService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static java.math.RoundingMode.HALF_UP;
import static ru.investbook.parser.tinkoff.TinkoffSecurityQuoteTable.SecurityQuoteTableHeader.*;
import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTableHelper.declareSecurity;

@Slf4j
public class TinkoffSecurityQuoteTable extends SingleAbstractReportTable<SecurityQuote> {

    private final SecurityCodeAndIsinTable codeAndIsin;
    private final ForeignExchangeRateService foreignExchangeRateService;
    private final BigDecimal oneHundred = BigDecimal.valueOf(100);
    private final LocalDate date;
    private BigDecimal rubSecuritiesTotalValue = BigDecimal.ZERO;
    private BigDecimal usdSecuritiesTotalValue = BigDecimal.ZERO;

    static TinkoffSecurityQuoteTable[] of(SingleBrokerReport report,
                                          SecurityCodeAndIsinTable codeAndIsin,
                                          ForeignExchangeRateService foreignExchangeRateService) {
        return new TinkoffSecurityQuoteTable[]{
                new TinkoffSecurityQuoteTable("3. Движение финансовых активов инвестора",
                        report, codeAndIsin, foreignExchangeRateService),
                new TinkoffSecurityQuoteTable("3.1 Движение по ценным бумагам инвестора",
                        report, codeAndIsin, foreignExchangeRateService)};
    }

    public TinkoffSecurityQuoteTable(String tableNamePrefix,
                                     SingleBrokerReport report,
                                     SecurityCodeAndIsinTable codeAndIsin,
                                     ForeignExchangeRateService foreignExchangeRateService) {
        super(report,
                (cell) -> cell.startsWith(tableNamePrefix),
                (cell) -> TinkoffBrokerReport.tablesLastRowPattern.matcher(cell).lookingAt(),
                SecurityQuoteTableHeader.class);
        this.codeAndIsin = codeAndIsin;
        this.foreignExchangeRateService = foreignExchangeRateService;
        this.date = LocalDate.ofInstant(report.getReportEndDateTime(), report.getReportZoneId());
    }

    @Override
    protected SecurityQuote parseRow(TableRow row) {
        adjustSecuritiesValueEstimate(row);

        BigDecimal price = row.getBigDecimalCellValueOrDefault(PRICE, null);
        if (price == null) {
            return null;
        }

        Optional<Security> security = getSecurityId(row);
        if (security.isEmpty()) {
            return null;
        }

        SecurityQuote.SecurityQuoteBuilder builder = SecurityQuote.builder()
                .security(declareSecurity(security.get(), getReport().getSecurityRegistrar()))
                .timestamp(getReport().getReportEndDateTime())
                .currency(row.getStringCellValue(CURRENCY));

        BigDecimal accruedInterest = row.getBigDecimalCellValueOrDefault(ACCRUED_INTEREST, BigDecimal.ZERO);
        boolean isBond = Math.abs(accruedInterest.floatValue()) > 1e-3;
        if (isBond) {
            String code = row.getStringCellValue(CODE);
            int count = row.getIntCellValue(COUNT);
            BigDecimal quote = price.multiply(oneHundred)
                    .divide(codeAndIsin.getFaceValue(code), 6, HALF_UP);
            BigDecimal accruedInterestPerBond = accruedInterest.divide(BigDecimal.valueOf(count), 6, HALF_UP);
            builder
                    .price(price)
                    .quote(quote)
                    .accruedInterest(accruedInterestPerBond);
        } else {
            builder.quote(price);
        }

        return builder.build();
    }

    private void adjustSecuritiesValueEstimate(TableRow row) {
        try {
            BigDecimal value = row.getBigDecimalCellValueOrDefault(VALUE, null);
            if (value != null) {
                String currency = row.getStringCellValue(CURRENCY);
                if (currency.equalsIgnoreCase("RUB")) {
                    rubSecuritiesTotalValue = rubSecuritiesTotalValue.add(value);
                } else if (currency.equalsIgnoreCase("USD")) {
                    usdSecuritiesTotalValue = usdSecuritiesTotalValue.add(value);
                } else {
                    if (Math.abs(value.floatValue()) > 1e-3) {
                        BigDecimal rate =
                                foreignExchangeRateService.getExchangeRateOrDefault(currency, "RUB", date);
                        rubSecuritiesTotalValue = rubSecuritiesTotalValue.add(value.multiply(rate));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Не смог учесть вклад ЦБ в стоимости портфеля", e);
        }
    }

    private Optional<Security> getSecurityId(TableRow row) {
        try {
            String code = row.getStringCellValueOrDefault(CODE, null);
            if (StringUtils.hasLength(code)) {
                BigDecimal accruedInterest = row.getBigDecimalCellValueOrDefault(ACCRUED_INTEREST, BigDecimal.ZERO);
                boolean isBond = accruedInterest.floatValue() > 1e-3;
                Security security = TinkoffSecurityTransactionTableHelper.getSecurity(
                        code,
                        codeAndIsin,
                        row.getStringCellValue(SHORT_NAME),
                        isBond ? SecurityType.BOND : SecurityType.STOCK);
                return Optional.of(security);
            }
        } catch (Exception ignore) {
        }
        return Optional.empty();
    }

    public BigDecimal getRubSecuritiesTotalValue() {
        initializeIfNeed();
        return rubSecuritiesTotalValue;
    }

    public BigDecimal getUsdSecuritiesTotalValue() {
        initializeIfNeed();
        return usdSecuritiesTotalValue;
    }

    @RequiredArgsConstructor
    protected enum SecurityQuoteTableHeader implements TableColumnDescription {
        SHORT_NAME("Сокращенное", "наименование", "актива"),
        CODE("Код", "актива"),
        COUNT("Исходящий", "остаток"),
        PRICE("Рыночная", "цена"), // на одну бумагу
        ACCRUED_INTEREST("НКД"), // на все бумаги исходящего остатка
        CURRENCY("Валюта", "цены"),
        VALUE("Рыночная", "стои", "мость"); // на все бумаги исходящего остатка с учетом НКД

        @Getter
        private final TableColumn column;

        SecurityQuoteTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}

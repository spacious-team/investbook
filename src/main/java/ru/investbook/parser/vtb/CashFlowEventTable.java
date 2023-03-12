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

package ru.investbook.parser.vtb;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import org.springframework.util.StringUtils;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.investbook.parser.vtb.CashFlowEventTable.VtbCashFlowTableHeader.*;

public class CashFlowEventTable extends SingleAbstractReportTable<CashFlowEventTable.CashFlowEvent> {
    private static final String TABLE_NAME = "Движение денежных средств";
    private boolean isSubaccountPaymentsRemoved = false;

    public CashFlowEventTable(SingleBrokerReport report) {
        super(report, TABLE_NAME, null, VtbCashFlowTableHeader.class);
    }

    @Override
    protected CashFlowEvent parseRow(TableRow row) {
        String operation = row.getStringCellValueOrDefault(OPERATION, null);
        if (operation == null) {
            return null;
        }
        return CashFlowEvent.builder()
                .date(row.getInstantCellValue(DATE))
                .operation(operation.toLowerCase().trim())
                .value(row.getBigDecimalCellValue(VALUE))
                .currency(VtbBrokerReport.convertToCurrency(row.getStringCellValue(CURRENCY)))
                .description(row.getStringCellValueOrDefault(DESCRIPTION, ""))
                .build();
    }

    @Override
    public List<CashFlowEvent> getData() {
        List<CashFlowEvent> data = super.getData();
        if (!isSubaccountPaymentsRemoved) {
            // gh-170: дивиденды и купоны субсчета, проходят через основный счет.
            // Для основного счета удаляем события выплаты + перечисления на субсчет
            Collection<CashFlowEvent> filteredData = new ArrayList<>(data.size());
            for (CashFlowEvent event : data) {
                if (data.stream().noneMatch(e -> e.isSubaccountPaymentEvent(event))) {
                    filteredData.add(event);
                }
            }
            data.clear();
            data.addAll(filteredData);
            isSubaccountPaymentsRemoved = true;
        }
        return data;
    }

    @Getter
    @Slf4j
    @Builder
    @EqualsAndHashCode
    static class CashFlowEvent {
        private static final String duplicateOperation = "Перераспределение дохода между субсчетами / торговыми площадками";
        private final Instant date;
        private final BigDecimal value;
        private final String currency;
        private final String operation;
        private final String description;
        private String lowercaseDescription;

        boolean isSubaccountPaymentEvent(CashFlowEvent pairedEvent) {
            return date.equals(pairedEvent.date) &&
                    (duplicateOperation.equals(operation) || duplicateOperation.equals(pairedEvent.operation)) &&
                    value.equals(pairedEvent.value.negate()) &&
                    currency.equals(pairedEvent.currency) &&
                    (StringUtils.hasLength(description) ?
                            description.equals(pairedEvent.description) :
                            !StringUtils.hasLength(pairedEvent.description));
        }

        String getLowercaseDescription() {
            if (lowercaseDescription == null) {
                lowercaseDescription = description.toLowerCase();
            }
            return lowercaseDescription;
        }

        CashFlowType getEventType() {
            String lowercaseDescription = getLowercaseDescription();
            return switch (operation) {
                // gh-170
                case "дивиденды" -> CashFlowType.DIVIDEND;
                case "купонный доход" -> CashFlowType.COUPON;
                case "погашение ценных бумаг" -> {
                    if (lowercaseDescription.contains("част.погаш") || lowercaseDescription.contains("частичное досроч")) {
                        yield CashFlowType.AMORTIZATION;
                    } else if (lowercaseDescription.contains("погаш. номин.ст-ти обл")) { // предположение
                        yield CashFlowType.REDEMPTION;
                    }
                    throw new IllegalArgumentException("Неожиданное значение: " + description);
                }
                case "зачисление денежных средств" -> {
                    if (lowercaseDescription.contains("дивиденды")) {
                        yield CashFlowType.DIVIDEND;
                    } else if (lowercaseDescription.contains("погаш. номин.ст-ти обл")) {
                        yield CashFlowType.REDEMPTION;
                    } else if (lowercaseDescription.contains("част.погаш") || lowercaseDescription.contains("частичное досроч")) {
                        yield CashFlowType.AMORTIZATION;
                    } else if (lowercaseDescription.contains("куп. дох. по обл")) {
                        yield CashFlowType.COUPON;
                    } else {
                        yield CashFlowType.CASH;
                    }
                }
                case "списание денежных средств" -> CashFlowType.CASH;
                case "перевод денежных средств" -> CashFlowType.CASH; // перевод ДС на другой субсчет
                // выплаты субсчета проходят через основной счет
                case "перераспределение дохода между субсчетами / торговыми площадками" -> CashFlowType.CASH;
                case "ндфл" -> CashFlowType.TAX;
                case "вариационная маржа" -> CashFlowType.DERIVATIVE_PROFIT;
                default -> {
                    log.debug("Проигнорирована операция '{}': {}", operation, description);
                    yield null;
                }
            };
        }

    }

    @Getter
    enum VtbCashFlowTableHeader implements TableHeaderColumn {
        DATE("дата"),
        VALUE("сумма"),
        CURRENCY("валюта"),
        OPERATION("тип операции"),
        DESCRIPTION("комментарий");

        private final TableColumn column;

        VtbCashFlowTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}

/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.AbstractTable;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.uralsib.SecuritiesTable.ReportSecurityInformation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;
import static ru.investbook.parser.uralsib.PaymentsTable.PaymentsTableHeader.*;
import static ru.investbook.parser.uralsib.UralsibBrokerReport.convertToCurrency;

@Slf4j
abstract class PaymentsTable extends AbstractReportTable<SecurityEventCashFlow> {

    static final String TABLE_NAME = "ДВИЖЕНИЕ ДЕНЕЖНЫХ СРЕДСТВ ЗА ОТЧЕТНЫЙ ПЕРИОД";
    protected static final BigDecimal minValue = BigDecimal.valueOf(0.01);
    // human readable name -> incoming count
    private final List<ReportSecurityInformation> securitiesIncomingCount;
    private final List<SecurityTransaction> securityTransactions;
    private final Collection<EventCashFlow> eventCashFlows = new ArrayList<>();
    private final Pattern taxInformationPattern = Pattern.compile("налог в размере ([0-9.]+) удержан");
    private String currentRowDescription = "";

    public PaymentsTable(UralsibBrokerReport report,
                         SecuritiesTable securitiesTable,
                         ReportTable<SecurityTransaction> securityTransactionTable) {
        super(report, TABLE_NAME, "", PaymentsTableHeader.class);
        this.securitiesIncomingCount = securitiesTable.getData();
        this.securityTransactions = securityTransactionTable.getData();
    }

    @Override
    protected Collection<SecurityEventCashFlow> parseTable(Table table) {
        return table.getDataCollection(getReport().getPath(), this::getRowAndSaveDescription,
                this::checkEquality, this::mergeDuplicates);
    }

    private Collection<SecurityEventCashFlow> getRowAndSaveDescription(Table table, TableRow row) {
        // Тип операции = "Разблокировано средств ГО" имеет пустое описание, не падаем, возвращаем default
        currentRowDescription = table.getStringCellValueOrDefault(row, DESCRIPTION, null);
        return getRow(table, row);
    }

    /**
     * @return security if found, null otherwise
     */
    protected Security getSecurity(Table table, TableRow row, CashFlowType cashEventIfSecurityNotFound) {
        try {
            return getSecurityIfCan(table, row);
        } catch (Exception e) {
            EventCashFlow.EventCashFlowBuilder builder = EventCashFlow.builder()
                    .portfolio(getReport().getPortfolio())
                    .timestamp(getReport().convertToInstant(table.getStringCellValue(row, DATE)))
                    .currency(convertToCurrency(table.getStringCellValue(row, CURRENCY)))
                    .description(table.getStringCellValue(row, DESCRIPTION));
            BigDecimal tax = getTax(table, row);
            BigDecimal value = table.getCurrencyCellValue(row, VALUE)
                    .add(tax.abs());
            EventCashFlow cash = builder
                    .eventType(cashEventIfSecurityNotFound)
                    .value(value)
                    .build();
            AbstractTable.addWithEqualityChecker(cash, eventCashFlows,
                    EventCashFlow::checkEquality, EventCashFlow::mergeDuplicates);
            if (tax.abs().compareTo(minValue) >= 0) {
                EventCashFlow taxEventCash = builder
                        .eventType(CashFlowType.TAX)
                        .value(tax.negate())
                        .build();
                AbstractTable.addWithEqualityChecker(taxEventCash, eventCashFlows,
                        EventCashFlow::checkEquality, EventCashFlow::mergeDuplicates);
            }
            log.debug("Получена выплата по ценной бумаге, которой нет в портфеле: " + cash);
            return null;
        }
    }

    protected Security getSecurityIfCan(Table table, TableRow row) {
        String description = table.getStringCellValue(row, DESCRIPTION);
        String descriptionLowercase = description.toLowerCase();
        for (ReportSecurityInformation info : securitiesIncomingCount) {
            if (info == null) continue;
            Security security = info.getSecurity();
            if (contains(descriptionLowercase, info.getCfi()) ||   // dividend
                    (security != null && (contains(descriptionLowercase, security.getName()) ||  // coupon, amortization, redemption
                            contains(descriptionLowercase, security.getIsin())))) { // for future report changes
                return security;
            }
        }
        throw new RuntimeException("Не могу найти ISIN ценной бумаги в отчете брокера по событию:" + description);
    }

    private boolean contains(String description, String securityParameter) {
        return securityParameter != null && description.contains(securityParameter.toLowerCase());
    }

    protected BigDecimal getTax(Table table, TableRow row) {
        String description = table.getStringCellValue(row, DESCRIPTION);
        Matcher matcher = taxInformationPattern.matcher(description.toLowerCase());
        if (matcher.find()) {
            try {
                return BigDecimal.valueOf(parseDouble(matcher.group(1)));
            } catch (Exception e) {
                log.info("Не смогу выделить сумму налога из описания: {}", description);
            }
        }
        return BigDecimal.ZERO;
    }

    protected Integer getSecurityCount(Security security, Instant atInstant) {
        int count = securitiesIncomingCount.stream()
                .filter(i -> i.getSecurity().getIsin().equals(security.getIsin()))
                .map(ReportSecurityInformation::getIncomingCount)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Не найдено количество на начало периода отчета для ЦБ " + security));
        Collection<SecurityTransaction> transactions = securityTransactions.stream()
                .filter(t -> t.getIsin().equals(security.getIsin()))
                .sorted(Comparator.comparing(SecurityTransaction::getTimestamp))
                .collect(Collectors.toList());
        int prevCount = 0;
        for (SecurityTransaction transaction : transactions) {
            if (transaction.getTimestamp().isBefore(atInstant)) {
                prevCount = count;
                count += transaction.getCount();
            } else {
                break;
            }
        }
        if (count > 0) {
            return count;
        } else if (prevCount > 0) {
            // dividends, coupons payments was received after securities celling (count == 0),
            // returning securities quantity before celling
            return prevCount;
        } else {
            throw  new RuntimeException("Не определено количество ЦБ " + security + " на момент времени " + atInstant);
        }
    }

    public Collection<EventCashFlow> getEventCashFlows() {
        initializeIfNeed();
        return eventCashFlows;
    }

    @Override
    protected boolean checkEquality(SecurityEventCashFlow cash1, SecurityEventCashFlow cash2) {
        return SecurityEventCashFlow.checkEquality(cash1, cash2);
    }

    @Override
    protected Collection<SecurityEventCashFlow> mergeDuplicates(SecurityEventCashFlow cash1, SecurityEventCashFlow cash2) {
        // gh-78: обе выплаты должны быть сохранены. Одна выплата выполнена по текущему портфелю, другая - по связанному ИИС.
        // К сожалению, сохраняем обе выплаты как по внешнему портфелю, т.к. брокер по выплате не указал количество ЦБ
        // ни по одной из выплат, поэтому не возможно определить какая из выплат относится к текущему портфелю.
        AbstractTable.addWithEqualityChecker(cast(cash1), eventCashFlows,
                EventCashFlow::checkEquality, EventCashFlow::mergeDuplicates);
        AbstractTable.addWithEqualityChecker(cast(cash2), eventCashFlows,
                EventCashFlow::checkEquality, EventCashFlow::mergeDuplicates);
        return Collections.emptyList();
    }

    private EventCashFlow cast(SecurityEventCashFlow cash) {
        return EventCashFlow.builder()
                .portfolio(cash.getPortfolio())
                .timestamp(cash.getTimestamp())
                .eventType(cash.getEventType())
                .value(cash.getValue())
                .currency(cash.getCurrency())
                .description(currentRowDescription)
                .build();
    }

    enum PaymentsTableHeader implements TableColumnDescription {
        DATE("дата"),
        OPERATION("тип", "операции"),
        VALUE("сумма"),
        CURRENCY("валюта"),
        DESCRIPTION("комментарий");

        @Getter
        private final TableColumn column;

        PaymentsTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}

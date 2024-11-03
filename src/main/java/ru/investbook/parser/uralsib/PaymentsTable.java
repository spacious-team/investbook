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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.AbstractTable;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.uralsib.SecuritiesTable.ReportSecurityInformation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Character.isLetterOrDigit;
import static java.lang.Double.parseDouble;
import static java.util.Objects.requireNonNull;
import static ru.investbook.parser.uralsib.PaymentsTable.PaymentsTableHeader.*;
import static ru.investbook.parser.uralsib.UralsibBrokerReport.convertToCurrency;

@Slf4j
abstract class PaymentsTable extends SingleAbstractReportTable<SecurityEventCashFlow> {

    static final String TABLE_NAME = "ДВИЖЕНИЕ ДЕНЕЖНЫХ СРЕДСТВ ЗА ОТЧЕТНЫЙ ПЕРИОД";
    protected static final BigDecimal minValue = BigDecimal.valueOf(0.01);
    // human-readable name -> incoming count
    private final List<ReportSecurityInformation> securitiesIncomingCount;
    private final List<SecurityTransaction> securityTransactions;
    private final Collection<EventCashFlow> eventCashFlows = new ArrayList<>();
    private final Pattern taxInformationPattern = Pattern.compile("налог в размере ([0-9.]+) удержан");
    private @Nullable String currentRowDescription = "";

    public PaymentsTable(UralsibBrokerReport report,
                         SecuritiesTable securitiesTable,
                         ReportTable<SecurityTransaction> securityTransactionTable) {
        super(report, TABLE_NAME, "", PaymentsTableHeader.class);
        this.securitiesIncomingCount = securitiesTable.getData();
        this.securityTransactions = securityTransactionTable.getData();
    }

    @Override
    protected Collection<SecurityEventCashFlow> parseTable(Table table) {
        return table.getDataCollection(getReport(), this::getRowAndSaveDescription,
                this::checkEquality, this::mergeDuplicates);
    }

    private Collection<SecurityEventCashFlow> getRowAndSaveDescription(TableRow row) {
        // Тип операции = "Разблокировано средств ГО" имеет пустое описание, не падаем, возвращаем default
        currentRowDescription = row.getStringCellValueOrDefault(DESCRIPTION, null);
        return parseRowToCollection(row);
    }

    /**
     * @return security if found, null otherwise
     */
    protected @Nullable Security getSecurity(TableRow row, CashFlowType cashEventIfSecurityNotFound) {
        try {
            return getSecurityIfCan(row);
        } catch (Exception e) {
            EventCashFlow.EventCashFlowBuilder builder = EventCashFlow.builder()
                    .portfolio(getReport().getPortfolio())
                    .timestamp(convertToInstant(row.getStringCellValue(DATE)))
                    .currency(convertToCurrency(row.getStringCellValue(CURRENCY)))
                    .description(row.getStringCellValueOrDefault(DESCRIPTION, null));
            BigDecimal tax = getTax(row);
            BigDecimal value = row.getBigDecimalCellValue(VALUE)
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

    protected Security getSecurityIfCan(TableRow row) {
        String description = row.getStringCellValueOrDefault(DESCRIPTION, "");
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

    private boolean contains(String description, @Nullable String securityParameter) {
        if (securityParameter == null) {
            return false;
        }
        int start = description.indexOf(securityParameter.toLowerCase());
        if (start < 0) {
            return false;
        }
        int end = start + securityParameter.length(); // exclusive
        boolean leftWordBoundary = (start == 0 || !isLetterOrDigit(description.charAt(start - 1)));
        boolean rightWordBoundary = (end == description.length() || !isLetterOrDigit(description.charAt(end)));
        return leftWordBoundary && rightWordBoundary;
    }

    protected BigDecimal getTax(TableRow row) {
        String description = row.getStringCellValueOrDefault(DESCRIPTION, "");
        Matcher matcher = taxInformationPattern.matcher(description.toLowerCase());
        if (matcher.find()) {
            try {
                String value = requireNonNull(matcher.group(1));
                return BigDecimal.valueOf(parseDouble(value));
            } catch (Exception e) {
                log.info("Не смогу выделить сумму налога из описания: {}", description);
            }
        }
        return BigDecimal.ZERO;
    }

    protected Integer getSecurityCount(Security security, Instant atInstant) {
        int count = securitiesIncomingCount.stream()
                .filter(i -> Objects.equals(i.getSecurity().getId(), security.getId()))
                .map(ReportSecurityInformation::getIncomingCount)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Не найдено количество на начало периода отчета для ЦБ " + security));
        Collection<SecurityTransaction> transactions = securityTransactions.stream()
                .filter(t -> Objects.equals(t.getSecurity(), security.getId()))
                .sorted(Comparator.comparing(SecurityTransaction::getTimestamp))
                .toList();
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

    enum PaymentsTableHeader implements TableHeaderColumn {
        DATE("дата"),
        OPERATION("тип", "операции"),
        VALUE("сумма"),
        CURRENCY("валюта"),
        DESCRIPTION("комментарий");

        @Getter
        private final TableColumn column;

        PaymentsTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}

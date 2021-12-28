/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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

import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static ru.investbook.parser.vtb.VtbSecurityFlowTable.VtbSecurityFlowTableHeader.*;

public class VtbSecurityDepositAndWithdrawalTable  extends SingleAbstractReportTable<SecurityTransaction> {

    static final String TABLE_NAME = "Движение ценных бумаг";

    private final Map<String, Integer> bondRedemptions = new HashMap<>(1);
    private final Set<String> generatedTradeIds = new HashSet<>();

    protected VtbSecurityDepositAndWithdrawalTable(SingleBrokerReport report) {
        super(report, TABLE_NAME, null, VtbSecurityFlowTable.VtbSecurityFlowTableHeader.class);
    }

    @Override
    protected SecurityTransaction parseRow(TableRow row) {
        String operation = row.getStringCellValueOrDefault(OPERATION, "").toLowerCase().trim();
        switch (operation) {
            case "перевод цб": // перевод между субсчетами
            case "конвертация цб":
            case "вывод цб": // указывался при сплите акций APPL 4:1 (count = +3) и TSLA 5:1 (count = +4)
            case "ввод цб": // догадка, нет примера отчета
            case "зачисление цб": // gh-352, без примера
            case "списание цб":   // gh-352, без примера
                break;
            case "погашение цб":
                String isin = row.getStringCellValue(NAME_REGNUMBER_ISIN).split(",")[2].trim();
                Integer count = Math.abs(row.getIntCellValue(COUNT));
                bondRedemptions.put(isin, count);
                return null;
            default:
                return null;
        }

        String portfolio = getReport().getPortfolio();
        String description = row.getStringCellValue(NAME_REGNUMBER_ISIN);
        Security security = VtbReportHelper.getSecurity(description);
        Instant timestamp = row.getInstantCellValue(DATE);
        String tradeId = generateTradeId(portfolio, timestamp, security.getIsin());
        int securityId = getReport().getSecurityRegistrar().declareStockOrBond(security.getIsin(), security::toBuilder);

        return SecurityTransaction.builder()
                        .tradeId(tradeId)
                        .timestamp(timestamp)
                        .portfolio(portfolio)
                        .security(securityId)
                        .count(row.getIntCellValue(COUNT))
                        .build();
    }

    private String generateTradeId(String portfolio, Instant instant, String isin) {
        long epochSecond = instant.getEpochSecond();
        for (int i = 0; i < 1000; i++) { // gh-395: maybe multiple deposit/withdrawal during day for same ISIN (for ex. share split)
            String id = (epochSecond + i) + isin + portfolio;
            String tradeId = id.substring(0, Math.min(32, id.length()));
            if (generatedTradeIds.add(tradeId)) {
                return tradeId;
            }
        }
        throw new RuntimeException("Can't generate trade id");
    }

    public Optional<Integer> getBondRedemptionCount(String isin) {
        initializeIfNeed();
        return Optional.ofNullable(bondRedemptions.get(isin));
    }
}

/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.parser.investbook;

import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.AbstractTransaction.AbstractTransactionBuilder;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.parser.SecurityRegistrar;
import ru.investbook.repository.SecurityRepository;

import java.math.BigDecimal;
import java.time.Instant;

import static ru.investbook.parser.investbook.AbstractInvestbookTable.InvestbookReportTableHeader.*;

public class InvestbookTransactionTable extends AbstractSecurityAwareInvestbookTable<AbstractTransaction> {

    protected InvestbookTransactionTable(BrokerReport report,
                                         SecurityRegistrar securityRegistrar,
                                         SecurityRepository securityRepository,
                                         SecurityConverter securityConverter) {
        super(report, securityRegistrar, securityRepository, securityConverter);
    }

    @Override
    protected AbstractTransaction parseRow(TableRow row) {
        String operation = row.getStringCellValue(OPERATION).toLowerCase();
        boolean isBuy;
        if (operation.contains("покупка")) {
            isBuy = true;
        } else if (operation.contains("продажа")) {
            isBuy = false;
        } else {
            return null;
        }

        BigDecimal price = row.getBigDecimalCellValue(PRICE).abs();
        int count = Math.abs(row.getIntCellValue(COUNT));
        BigDecimal value = getOptionalAmount(price, count, isBuy);

        String securityTickerNameOrIsin = row.getStringCellValue(TICKER_NAME_ISIN);
        SecurityType securityType = getSecurityTypeForTransaction(row, securityTickerNameOrIsin);


        AbstractTransactionBuilder<?, ?> builder = switch (securityType) {
            case STOCK, STOCK_OR_BOND, ASSET -> SecurityTransaction.builder()
                    .value(value)
                    .valueCurrency(row.getStringCellValue(CURRENCY));
            case BOND -> SecurityTransaction.builder()
                    .value(value)
                    .valueCurrency(row.getStringCellValue(CURRENCY))
                    .accruedInterest(getOptionalAmount(row.getBigDecimalCellValue(ACCRUED_INTEREST), count, isBuy));
            case DERIVATIVE -> DerivativeTransaction.builder()
                    .valueInPoints(value)
                    .value(getOptionalAmount(
                            row.getBigDecimalCellValueOrDefault(DERIVATIVE_PRICE_IN_CURRENCY, null),
                            count,
                            isBuy))
                    .valueCurrency(row.getStringCellValueOrDefault(CURRENCY, null));
            case CURRENCY_PAIR -> ForeignExchangeTransaction.builder()
                    .value(value)
                    .valueCurrency(row.getStringCellValue(CURRENCY));
        };
        String portfolio = row.getStringCellValue(PORTFOLIO);
        Instant timestamp = row.getInstantCellValue(DATE_TIME);
        int securityId = getSecurityIdForTransaction(securityTickerNameOrIsin, securityType);
        return builder
                .tradeId(getTradeId(portfolio, securityId, timestamp))
                .portfolio(portfolio)
                .timestamp(timestamp)
                .security(securityId)
                .count(count * (isBuy ? 1 : -1))
                .commission(getOptionalAmount(row.getBigDecimalCellValueOrDefault(FEE, null), 1, true))
                .commissionCurrency(row.getStringCellValueOrDefault(FEE_CURRENCY, null))
                .build();
    }

    private BigDecimal getOptionalAmount(BigDecimal price, int count, boolean isBuy) {
        if (price == null) return null;
        BigDecimal value = price.multiply(BigDecimal.valueOf(count));
        return isBuy ? value.negate() : value;
    }

    private SecurityType getSecurityTypeForTransaction(TableRow row, String securityTickerNameOrIsin) {
        SecurityType securityType;
        String derivativePrice = row.getStringCellValueOrDefault(DERIVATIVE_PRICE_IN_CURRENCY, null);
        if (derivativePrice != null) {
            securityType = SecurityType.DERIVATIVE;
        } else if (isCurrencyPair(securityTickerNameOrIsin)) {
            securityType = SecurityType.CURRENCY_PAIR;
        } else if (row.getBigDecimalCellValueOrDefault(ACCRUED_INTEREST, null) != null) {
            securityType = SecurityType.BOND;
        } else if (row.getBigDecimalCellValueOrDefault(PRICE, null) != null &&
                row.getBigDecimalCellValueOrDefault(FEE, null) == null) {
            securityType = SecurityType.ASSET;
        } else {
            securityType = SecurityType.STOCK;
        }
        return securityType;
    }

    private boolean isCurrencyPair(String securityTickerNameOrIsin) {
        return securityTickerNameOrIsin.length() == 10 &&
                securityTickerNameOrIsin.charAt(securityTickerNameOrIsin.length() - 4) == '_';
    }

    private int getSecurityIdForTransaction(String securityTickerNameOrIsin, SecurityType securityType) {
        return switch (securityType) {
            case STOCK -> getStockOrBondSecurityId(securityTickerNameOrIsin, SecurityType.STOCK);
            case BOND -> getStockOrBondSecurityId(securityTickerNameOrIsin, SecurityType.BOND);
            case DERIVATIVE -> getDerivativeSecurityId(securityTickerNameOrIsin);
            case CURRENCY_PAIR -> getCurrencyPairSecurityId(securityTickerNameOrIsin);
            case ASSET -> getAssetSecurityId(securityTickerNameOrIsin);
            default -> throw new IllegalArgumentException("Unexpected type: " + securityType);
        };
    }
}

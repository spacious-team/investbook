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

package ru.investbook.web.forms.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.xml.bind.DatatypeConverter;
import lombok.Data;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.springframework.util.StringUtils.hasText;

@Data
public class TransactionModel {

    private static final MessageDigest md; // not thread safe

    static {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private @Nullable Integer id;

    private @Nullable String tradeId;

    private @NotEmpty String portfolio;

    private Action action;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date = LocalDate.now();

    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime time = LocalTime.NOON;

    /**
     * In "name (isin)" or "contract-name" format
     */
    private @NotEmpty String security;

    private SecurityType securityType;

    private @Positive int count;

    /**
     * Equals to null for security deposit and withdrawal
     */
    private @Nullable @Positive BigDecimal price;

    private @Nullable @PositiveOrZero BigDecimal accruedInterest;

    /**
     * Price and accrued interest currency
     */
    private @NotEmpty String priceCurrency = "RUB";

    private @Nullable @Positive BigDecimal priceTick;

    private @Nullable @Positive BigDecimal priceTickValue;

    private @Nullable String priceTickValueCurrency = "RUB";

    /**
     * May be null for security deposit and withdrawal
     */
    private @Nullable @PositiveOrZero BigDecimal fee;

    private @NotEmpty String feeCurrency = "RUB";

    public enum Action {
        BUY, CELL
    }

    public void setPriceTickValueCurrency(@Nullable String priceTickValueCurrency) {
        if (priceTickValueCurrency != null) {
            this.priceTickValueCurrency = priceTickValueCurrency.toUpperCase();
        }
    }

    public void setPriceCurrency(String priceCurrency) {
        this.priceCurrency = priceCurrency.toUpperCase();
    }

    public void setFeeCurrency(String feeCurrency) {
        this.feeCurrency = feeCurrency.toUpperCase();
    }

    public void setSecurity(@Nullable String isin, @Nullable String securityName, SecurityType securityType) {
        this.security = SecurityHelper.getSecurityDescription(isin, securityName, securityType);
        this.securityType = securityType;
    }

    /**
     * Returns Name from template "Name (ISIN)" for stock and bond, code for derivative, securityName for asset
     */
    public String getSecurityName() {
        return SecurityHelper.getSecurityName(security, securityType);
    }

    /**
     * Returns ISIN if description in "Name (ISIN)" format, null otherwise
     */
    public @Nullable String getSecurityIsin() {
        return SecurityHelper.getSecurityIsin(security);
    }

    public String getTradeId() {
        if (!hasText(tradeId)) {
            setTradeId(createTradeId());
        }
        @SuppressWarnings("nullness")
        String nonNullTradeId = requireNonNull(tradeId, "Не задан trade-id");
        return nonNullTradeId;
    }

    private String createTradeId() {
        //noinspection ConstantValue
        Assert.isTrue(portfolio != null && security != null && date != null && action != null,
                "Невалидные данные, ошибка вычисления trade-id");
        String string = portfolio.replaceAll(" ", "") +
                security.replaceAll(" ", "") +
                date +
                action.name() +
                count;
        synchronized (TransactionModel.class) {
            try {
                md.update(string.getBytes(UTF_8));
                return DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
            } finally {
                md.reset();
            }
        }
    }

    public boolean hasDerivativeTickValue() {
        return priceTick != null && priceTick.floatValue() > 0.000001 &&
                priceTickValue != null && priceTickValue.floatValue() > 0.000001 &&
                hasText(priceTickValueCurrency);
    }
}

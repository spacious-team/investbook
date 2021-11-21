/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

import static ru.investbook.web.forms.model.SecurityType.BOND;
import static ru.investbook.web.forms.model.SecurityType.SHARE;

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

    @Nullable
    private Integer id;

    @Nullable
    private String tradeId;

    @NotEmpty
    private String portfolio;

    @NotNull
    private Action action;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date = LocalDate.now();

    /**
     * In "name (isin)" or "contract-name" format
     */
    @NotEmpty
    private String security;

    @NotNull
    private SecurityType securityType;

    @NotNull
    @Positive
    private int count;

    /**
     * May be null for security deposit and withdrawal
     */
    @Nullable
    @Positive
    private BigDecimal price;

    @Nullable
    @Positive
    private BigDecimal accruedInterest;

    /**
     * Price and accrued interest currency
     */
    @NotEmpty
    private String priceCurrency = "RUB";

    @Nullable
    @Positive
    private BigDecimal priceTick;

    @Nullable
    @Positive
    private BigDecimal priceTickValue;

    @Nullable
    private String priceTickValueCurrency = "RUB";

    /**
     * May be null for security deposit and withdrawal
     */
    @Nullable
    @Positive
    private BigDecimal commission;

    @NotEmpty
    private String commissionCurrency = "RUB";

    public enum Action {
        BUY, CELL
    }

    public void setPriceTickValueCurrency(String priceTickValueCurrency) {
        if (priceTickValueCurrency != null) {
            this.priceTickValueCurrency = priceTickValueCurrency.toUpperCase();
        }
    }

    public void setPriceCurrency(String priceCurrency) {
        this.priceCurrency = priceCurrency.toUpperCase();
    }

    public void setCommissionCurrency(String commissionCurrency) {
        this.commissionCurrency = commissionCurrency.toUpperCase();
    }

    public void setSecurity(String securityId, String securityName) {
        this.security = SecurityHelper.getSecurityDescription(securityId, securityName);
    }

    public String getSecurityDisplayName() {
        return SecurityHelper.getSecurityDisplayName(security);
    }

    /**
     * Returns ISIN (stock market) or contract name (derivatives and forex market)
     */
    public String getSecurityId() {
        if (securityType == SHARE || securityType == BOND) {
            String id = SecurityHelper.getSecurityId(security);
            if (id == null || id.equals(security)) {
                throw new RuntimeException("В скобках должен быть указан ISIN инструмента: " + security);
            }
            return id;
        }
        return security;
    }

    /**
     * Returns security name (stock market) or null (derivatives and forex market)
     */
    public String getSecurityName() {
        if (securityType == SHARE || securityType == BOND) {
            String name = SecurityHelper.getSecurityName(security);
            if (name == null) {
                throw new RuntimeException("В скобках должен быть указан ISIN инструмента: " + security);
            }
            return name;
        }
        return null;
    }

    public String getTradeId() {
        if (!StringUtils.hasText(tradeId)) {
            setTradeId(createTradeId());
        }
        return tradeId;
    }

    private String createTradeId() {
        if (portfolio == null || security == null || date == null || action == null) {
            return null;
        }
        String string = portfolio.replaceAll(" ", "") +
                security.replaceAll(" ", "") +
                date +
                action.name() +
                count;
        synchronized (TransactionModel.class) {
            try {
                md.update(string.getBytes(StandardCharsets.UTF_8));
                return DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
            } finally {
                md.reset();
            }
        }
    }

    public boolean hasDerivativeTickValue() {
        return getPriceTick() != null && getPriceTick().floatValue() > 0.000001 &&
                getPriceTickValue() != null && getPriceTickValue().floatValue() > 0.000001 &&
                StringUtils.hasText(getPriceTickValueCurrency());
    }
}

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

package ru.investbook.parser.investbook;

import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.parser.SecurityRegistrar;
import ru.investbook.repository.SecurityRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class AbstractSecurityAwareInvestbookTable<RowType> extends AbstractInvestbookTable<RowType> {

    private final SecurityRegistrar securityRegistrar;
    protected final SecurityRepository securityRepository;
    protected final SecurityConverter securityConverter;
    private final Set<String> generatedTradeIds = new HashSet<>();

    protected AbstractSecurityAwareInvestbookTable(InvestbookBrokerReport report,
                                                   SecurityRegistrar securityRegistrar,
                                                   SecurityRepository securityRepository,
                                                   SecurityConverter securityConverter) {
        super(report);
        this.securityRegistrar = securityRegistrar;
        this.securityRepository = securityRepository;
        this.securityConverter = securityConverter;
    }

    protected int getDerivativeSecurityId(String ticker) {
        return securityRegistrar.declareDerivative(ticker);
    }

    /**
     * @param contract in USDRUB_TOM form
     * @return security ID
     */
    protected int getCurrencyPairSecurityId(String contract) {
        return securityRegistrar.declareCurrencyPair(contract);
    }

    protected int getAssetSecurityId(String assetName) {
        return securityRegistrar.declareAsset(assetName, () -> Security.builder().name(assetName));
    }

    protected int getStockOrBondSecurityId(String securityTickerNameOrIsin, SecurityType securityType) {
        Optional<SecurityEntity> optionalSecurity = (securityTickerNameOrIsin.length() == 12) ?
                securityRepository.findByIsin(securityTickerNameOrIsin) :
                Optional.empty();
        return optionalSecurity
                .or(() -> securityRepository.findByTicker(securityTickerNameOrIsin))
                .or(() -> securityRepository.findByName(securityTickerNameOrIsin))
                .map(SecurityEntity::getId)
                .orElseGet(() -> createStockOrBond(securityTickerNameOrIsin, securityType).getId());
    }

    private SecurityEntity createStockOrBond(String securityTickerNameOrIsin, SecurityType securityType) {
        Security.SecurityBuilder builder = Security.builder().type(securityType);
        if (securityType == SecurityType.STOCK && securityTickerNameOrIsin.length() <= 4 &&
                Character.isUpperCase(securityTickerNameOrIsin.charAt(securityTickerNameOrIsin.length() - 1))) {
            builder.ticker(securityTickerNameOrIsin);
        } else if (securityTickerNameOrIsin.length() == 12 &&
                Character.isDigit(securityTickerNameOrIsin.charAt(securityTickerNameOrIsin.length() - 1))) {
            builder.isin(securityTickerNameOrIsin);
        } else {
            builder.name(securityTickerNameOrIsin);
        }
        return securityRepository.save(securityConverter.toEntity(builder.build()));
    }

    protected String getTradeId(String portfolio, int securityId, Instant instant) {
        String tradeId;
        int counter = 0; // for spit deposit and withdrawal events have same timestamp
        do {
            tradeId = (instant.getEpochSecond() + (counter++)) + securityId + portfolio;
            tradeId = tradeId.substring(0, Math.min(32, tradeId.length()));
        } while (!generatedTradeIds.add(tradeId));
        return tradeId;
    }
}

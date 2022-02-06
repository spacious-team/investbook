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

package ru.investbook.openformat.v1_0_0;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import org.springframework.stereotype.Service;
import ru.investbook.parser.InvestbookApiClient;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioOpenFormatPersister {
    private final InvestbookApiClient api;

    public void persist(PortfolioOpenFormatV1_0_0 object) {

        object.getAccounts()
                .stream()
                .map(AccountPof::toPortfolio)
                .flatMap(Optional::stream)
                .forEach(api::addPortfolio);

        Collection<Security> securities = object.getAssets()
                .stream()
                .map(AssetPof::toSecurity)
                .flatMap(Optional::stream)
                .toList();
        securities.forEach(api::addSecurity);

        Map<Integer, String> accountToPortfolioId = object.getAccounts()
                .stream()
                .collect(Collectors.toMap(AccountPof::getId, AccountPof::getAccountNumber));
        Map<Integer, SecurityType> assetTypes = securities.stream()
                .collect(Collectors.toMap(Security::getId, Security::getType));

        object.getTrades()
                .stream()
                .map(t -> t.toTransaction(accountToPortfolioId))
                .flatMap(Optional::stream)
                .forEach(api::addTransaction);

        object.getTrades()
                .stream()
                .map(t -> t.getTransactionCashFlow(assetTypes))
                .flatMap(Collection::stream)
                .forEach(api::addTransactionCashFlow);

        object.getTransfer()
                .stream()
                .map(t -> t.toTransaction(accountToPortfolioId))
                .flatMap(Optional::stream)
                .forEach(api::addTransaction);

        object.getTransfer()
                .stream()
                .map(t -> t.getSecurityEventCashFlow(accountToPortfolioId))
                .flatMap(Collection::stream)
                .forEach(api::addSecurityEventCashFlow);

        object.getPayments()
                .stream()
                .map(t -> t.getSecurityEventCashFlow(accountToPortfolioId))
                .flatMap(Collection::stream)
                .forEach(api::addSecurityEventCashFlow);

        object.getCashFlows()
                .stream()
                .map(c -> c.toEventCashFlow(accountToPortfolioId))
                .flatMap(Optional::stream)
                .forEach(api::addEventCashFlow);

        VndInvestbookPof vndInvestbook = object.getVndInvestbook();
        if (vndInvestbook != null) {
            vndInvestbook.getPortfolioCash().forEach(api::addPortfolioCash);
            vndInvestbook.getPortfolioProperties().forEach(api::addPortfolioProperty);
            vndInvestbook.getSecurityDescriptions().forEach(api::addSecurityDescription);
            vndInvestbook.getSecurityQuotes().forEach(api::addSecurityQuote);
        }
    }
}

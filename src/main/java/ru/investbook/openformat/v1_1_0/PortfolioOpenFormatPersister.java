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

package ru.investbook.openformat.v1_1_0;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import org.springframework.stereotype.Service;
import ru.investbook.parser.InvestbookApiClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newWorkStealingPool;

@Service
@RequiredArgsConstructor
public class PortfolioOpenFormatPersister {
    private final InvestbookApiClient api;

    public void persist(PortfolioOpenFormatV1_1_0 object) {
        Collection<Runnable> tasks = new ArrayList<>();

        object.getAccounts()
                .stream()
                .map(AccountPof::toPortfolio)
                .flatMap(Optional::stream)
                .forEach(api::addPortfolio);

        Collection<Security> securities = object.getAssets()
                .parallelStream()
                .map(AssetPof::toSecurity)
                .flatMap(Optional::stream)
                .toList();
        securities.forEach(api::addSecurity);

        Map<Integer, String> accountToPortfolioId = object.getAccounts()
                .stream()
                .collect(Collectors.toMap(
                        AccountPof::getId,
                        a -> Optional.ofNullable(a.getAccountNumber()).orElse(String.valueOf(a.getId()))));
        Map<Integer, SecurityType> assetTypes = securities.stream()
                .collect(Collectors.toMap(Security::getId, Security::getType));

        tasks.add(() -> object.getTrades()
                .parallelStream()
                .map(t -> t.toTransaction(accountToPortfolioId, assetTypes))
                .flatMap(Optional::stream)
                .forEach(api::addTransaction));

        tasks.add(() -> object.getTransfer()
                .parallelStream()
                .map(t -> t.toTransaction(accountToPortfolioId))
                .flatMap(Optional::stream)
                .forEach(api::addTransaction));

        tasks.add(() -> object.getTransfer()
                .parallelStream()
                .map(t -> t.getSecurityEventCashFlow(accountToPortfolioId))
                .flatMap(Collection::stream)
                .forEach(api::addSecurityEventCashFlow));

        tasks.add(() -> object.getPayments()
                .parallelStream()
                .map(t -> t.getSecurityEventCashFlow(accountToPortfolioId, assetTypes))
                .flatMap(Collection::stream)
                .forEach(api::addSecurityEventCashFlow));

        tasks.add(() -> object.getCashFlows()
                .parallelStream()
                .map(c -> c.toEventCashFlow(accountToPortfolioId))
                .flatMap(Optional::stream)
                .forEach(api::addEventCashFlow));

        VndInvestbookPof vndInvestbook = object.getVndInvestbook();
        if (vndInvestbook != null) {
            tasks.add(() -> vndInvestbook.getPortfolioCash().forEach(api::addPortfolioCash));
            tasks.add(() -> vndInvestbook.getPortfolioProperties().forEach(api::addPortfolioProperty));
            tasks.add(() -> vndInvestbook.getSecurityDescriptions().forEach(api::addSecurityDescription));
            tasks.add(() -> vndInvestbook.getSecurityQuotes().forEach(api::addSecurityQuote));
        }

        runTasks(tasks);
    }

    @SneakyThrows
    private void runTasks(Collection<Runnable> tasks) {
        ExecutorService executorService = newWorkStealingPool(4 * Runtime.getRuntime().availableProcessors());
        try {
            Collection<Callable<Object>> callables = tasks.stream()
                    .map(Executors::callable)
                    .toList();
            executorService.invokeAll(callables);
        } finally {
            executorService.shutdown();
        }
    }
}

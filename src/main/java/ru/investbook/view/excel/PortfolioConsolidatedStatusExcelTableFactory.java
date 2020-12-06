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

package ru.investbook.view.excel;

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioPropertyConverter;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.SecurityQuoteRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.view.ForeignExchangeRateService;
import ru.investbook.view.PositionsFactory;
import ru.investbook.view.Table;

import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PortfolioConsolidatedStatusExcelTableFactory extends PortfolioStatusExcelTableFactory {

    public PortfolioConsolidatedStatusExcelTableFactory(TransactionRepository transactionRepository,
                                                        SecurityRepository securityRepository,
                                                        TransactionCashFlowRepository transactionCashFlowRepository,
                                                        SecurityEventCashFlowRepository securityEventCashFlowRepository,
                                                        SecurityQuoteRepository securityQuoteRepository,
                                                        SecurityConverter securityConverter,
                                                        PortfolioPropertyConverter portfolioPropertyConverter,
                                                        PositionsFactory positionsFactory,
                                                        ForeignExchangeRateService foreignExchangeRateService,
                                                        PortfolioPropertyRepository portfolioPropertyRepository) {
        super(transactionRepository, securityRepository, transactionCashFlowRepository, securityEventCashFlowRepository,
                securityQuoteRepository, securityConverter, portfolioPropertyConverter, positionsFactory,
                foreignExchangeRateService, portfolioPropertyRepository);
    }

    public Table create(String forCurrency) {
        Table table = new Table(); //create(Portfolio.ALL, getSecuritiesIsin(forCurrency));
        table.add(getCashRow(Portfolio.ALL, forCurrency));
        return table;
    }

    /**
     * Возвращает для всех портфелей сумму последних известных остатков денежных средств соответствующих дате, не позже указанной.
     */
    @Override
    protected Collection<PortfolioProperty> getPortfolioCash(Portfolio portfolio, Instant atInstant) {
        return portfolioPropertyRepository
                .findDistinctOnPortfolioByPropertyAndTimestampBetweenOrderByTimestampDesc(
                        PortfolioPropertyType.CASH.name(),
                        Instant.ofEpochSecond(0),
                        atInstant)
                .stream()
                .map(portfolioPropertyConverter::fromEntity)
                .collect(Collectors.toList());
    }
}

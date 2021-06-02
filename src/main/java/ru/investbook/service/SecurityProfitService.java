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

package ru.investbook.service;

import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.Transaction;
import ru.investbook.report.FifoPositions;
import ru.investbook.report.ViewFilter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;

public interface SecurityProfitService {

    Optional<Instant> getLastEventTimestamp(Collection<String> portfolios, Security security, Set<Integer> events, ViewFilter filter);

    /**
     * Курсовой доход с купли-продажи (для деривативов - суммарная вариационная маржа)
     */
    BigDecimal getGrossProfit(Collection<String> portfolios, Security security, FifoPositions positions, String toCurrency);

    /**
     * Разница доходов с продажи и расходов на покупку
     */
    BigDecimal getPurchaseCost(Security security, FifoPositions positions, String toCurrency);

    /**
     * Разница проданного и купленного НКД
     */
    BigDecimal getPurchaseAccruedInterest(Security security, FifoPositions positions, String toCurrency);

    BigDecimal getTotal(Deque<Transaction> transactions, CashFlowType type, String toCurrency);

    BigDecimal sumPaymentsForType(Collection<String> portfolios, Security security, CashFlowType cashFlowType, String toCurrency);

    /**
     * Возвращает для портфеля последний известный остаток денежных средств соответствующей дате, не позже указанной.
     * Если портфель не указан, возвращает для всех портфелей сумму последних известных остатков денежных средств
     * соответствующих дате, не позже указанной.
     */
    Collection<PortfolioProperty> getPortfolioCash(Collection<String> portfolios, Instant atInstant);
}

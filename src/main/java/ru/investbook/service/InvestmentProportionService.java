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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.springframework.stereotype.Service;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityDescriptionEntity;
import ru.investbook.report.FifoPositions;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.SecurityDescriptionRepository;
import ru.investbook.repository.SecurityRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.System.nanoTime;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static org.spacious_team.broker.pojo.SecurityType.STOCK_OR_BOND;
import static org.spacious_team.broker.pojo.SecurityType.getSecurityType;
import static ru.investbook.report.ForeignExchangeRateService.RUB;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentProportionService {

    private final SecurityRepository securityRepository;
    private final SecurityConverter securityConverter;
    private final SecurityDescriptionRepository securityDescriptionRepository;
    private final FifoPositionsFactory fifoPositionsFactory;
    private final SecurityProfitService securityProfitService;

    public Map<String, Float> getSectorProportions(ViewFilter filter) {
        try {
            long t0 = nanoTime();
            Map<String, Float> result = securityRepository.findAll()
                    .stream()
                    .filter(security -> getSecurityType(security.getId()) == STOCK_OR_BOND)
                    .map(securityConverter::fromEntity)
                    .map(security -> getSecurityToInvestment(security, filter))
                    .filter(v -> v.investment().floatValue() > 1)
                    .collect(Collectors.groupingBy(this::getEconomicSector,
                            mapping(SecurityInvestment::getInvestment, reducing(0f, Float::sum))));
            log.info("Рассчитаны объемы инвестиций в сектора экономики за {}", Duration.ofNanos(nanoTime() - t0));
            return result;
        } catch (Exception e) {
            String message = "Ошибка при расчете объемов инвестиций в сектора экономики";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private SecurityInvestment getSecurityToInvestment(Security security, ViewFilter filter) {
        FifoPositions positions = fifoPositionsFactory.get(filter.getPortfolios(), security, filter);
        int openedPositions = positions.getCurrentOpenedPositionsCount();
        BigDecimal investmentRub = ofNullable(securityProfitService.getSecurityQuote(security, RUB, filter))
                .map(SecurityQuote::getDirtyPriceInCurrency)
                .map(quote -> quote.multiply(BigDecimal.valueOf(openedPositions)))
                .orElseGet(() -> securityProfitService.getPurchaseCost(security, positions, RUB)
                        .add(securityProfitService.getPurchaseAccruedInterest(security, positions, RUB)));
        return new SecurityInvestment(security, investmentRub);
    }

    private String getEconomicSector(SecurityInvestment securityInvestment) {
        return securityDescriptionRepository.findById(securityInvestment.security().getId())
                .map(SecurityDescriptionEntity::getSector)
                .orElse(SecuritySectorService.UNKNOWN_SECTOR);
    }

    private static record SecurityInvestment(Security security, BigDecimal investment) {
        private float getInvestment() {
            return investment == null ? 0 : investment.floatValue();
        }
    }
}

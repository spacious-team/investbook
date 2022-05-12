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

package ru.investbook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import org.springframework.stereotype.Service;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityDescriptionEntity;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.report.FifoPositionsFilter;
import ru.investbook.repository.SecurityDescriptionRepository;
import ru.investbook.repository.SecurityRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.System.nanoTime;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static org.spacious_team.broker.pojo.SecurityType.*;
import static ru.investbook.report.ForeignExchangeRateService.RUB;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentProportionService {

    private final Set<SecurityType> stockAndBondTypes = Set.of(STOCK, BOND, STOCK_OR_BOND);
    private final SecurityRepository securityRepository;
    private final SecurityConverter securityConverter;
    private final SecurityDescriptionRepository securityDescriptionRepository;
    private final FifoPositionsFactory fifoPositionsFactory;
    private final SecurityProfitService securityProfitService;

    public Map<String, Float> getSectorProportions(Set<String> portfolios) {
        try {
            long t0 = nanoTime();
            FifoPositionsFilter filter = FifoPositionsFilter.of(portfolios);
            Map<String, Float> result = securityRepository.findByTypeIn(stockAndBondTypes)
                    .stream()
                    .map(securityConverter::fromEntity)
                    .map(security -> getInvestmentAmount(security, filter))
                    .flatMap(Optional::stream)
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

    private Optional<SecurityInvestment> getInvestmentAmount(Security security, FifoPositionsFilter filter) {
        return getOpenedPositionsCostByCurrentOrLastTransactionQuoteInRub(security, filter)
                .map(investmentRub -> new SecurityInvestment(security, investmentRub));
    }

    public Optional<BigDecimal> getOpenedPositionsCostByCurrentOrLastTransactionQuoteInRub(Security security,
                                                                                           FifoPositionsFilter filter) {
        return ofNullable(securityProfitService.getSecurityQuote(security, RUB, filter.getToDate()))
                .map(quote -> quote.getDirtyPriceInCurrency(security.getType() == SecurityType.DERIVATIVE))
                .or(() -> securityProfitService.getSecurityQuoteFromLastTransaction(security, RUB))
                .map(quote -> quote.multiply(
                        BigDecimal.valueOf(
                                fifoPositionsFactory.get(security, filter)
                                        .getCurrentOpenedPositionsCount())));
    }

    private String getEconomicSector(SecurityInvestment securityInvestment) {
        return securityDescriptionRepository.findById(securityInvestment.security().getId())
                .map(SecurityDescriptionEntity::getSector)
                .orElse(SecuritySectorService.UNKNOWN_SECTOR);
    }

    private record SecurityInvestment(Security security, BigDecimal investment) {
        private float getInvestment() {
            return investment == null ? 0 : investment.floatValue();
        }
    }
}

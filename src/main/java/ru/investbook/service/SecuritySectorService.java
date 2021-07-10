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
import org.spacious_team.broker.pojo.SecurityDescription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.converter.SecurityDescriptionConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.repository.SecurityDescriptionRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.service.moex.MoexIssClient;
import ru.investbook.service.smartlab.SmartlabShareSectors;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.System.nanoTime;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.spacious_team.broker.pojo.SecurityType.STOCK_OR_BOND;
import static org.spacious_team.broker.pojo.SecurityType.getSecurityType;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecuritySectorService {

    public static final String UNKNOWN_SECTOR = "Другое";
    private final SecurityRepository securityRepository;
    private final SecurityDescriptionRepository securityDescriptionRepository;
    private final SecurityDescriptionConverter securityDescriptionConverter;
    private final SmartlabShareSectors smartlabShareSectors;
    private final MoexIssClient moexIssClient;

    @Transactional
    public void uploadAndUpdateSecuritySectors() {
        try {
            long t0 = nanoTime();
            Map<String, String> tickerToSector = getTickerToSectorIndex();
            securityRepository.findAll()
                    .stream()
                    .map(entity -> getSecuritySector(tickerToSector, entity))
                    .flatMap(Optional::stream)
                    .map(securityDescriptionConverter::toEntity)
                    .forEach(desc -> securityDescriptionRepository.
                            createOrUpdateSector(desc.getSecurity(), desc.getSector()));
            log.info("Справочник секторов экономики обновлен за {}", Duration.ofNanos(nanoTime() - t0));
        } catch (Exception e) {
            log.error("Ошибка обновления информации о секторах экономики", e);
            throw new RuntimeException("Ошибка обновления информации о секторах экономики", e);
        }
    }

    private Map<String, String> getTickerToSectorIndex() {
        Map<String, List<String>> sectorToShares = smartlabShareSectors.getShareSectors();
        return sectorToShares.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue()
                        .stream()
                        .map(ticker -> new TickerAndSector(ticker.toUpperCase(), entry.getKey())))
                .collect(toMap(TickerAndSector::ticker, TickerAndSector::sector));
    }

    private static record TickerAndSector(String ticker, String sector) {
    }

    private Optional<SecurityDescription> getSecuritySector(Map<String, String> tickerToSector, SecurityEntity security) {
        String sector = ofNullable(security.getTicker())
                .map(String::toUpperCase)
                .map(tickerToSector::get)
                .or(() -> ofNullable(security.getName()) // may be ticker saved by broker in name?
                        .map(String::toUpperCase)
                        .map(tickerToSector::get))
                .or(() -> ofNullable(security.getIsin())
                        .or(() -> ofNullable(security.getId()))
                        .flatMap(moexIssClient::getSecId) // for shares moex secId returns ticker
                        .map(t -> t.endsWith("-RM") ? t.substring(0, t.length() - 2) : t)
                        .map(String::toUpperCase)
                        .map(tickerToSector::get))
                .or(() -> ofNullable(security.getId())
                        .filter(id -> getSecurityType(id) == STOCK_OR_BOND)
                        .map($ -> UNKNOWN_SECTOR))
                .orElse(null);
        if (sector == null) {
            return empty();
        }
        return Optional.of(SecurityDescription.builder()
                .security(security.getId())
                .sector(sector)
                .build());
    }
}

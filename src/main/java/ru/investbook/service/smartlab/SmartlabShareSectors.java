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

package ru.investbook.service.smartlab;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static org.springframework.util.StringUtils.capitalize;

@Service
@Slf4j
public class SmartlabShareSectors {

    private static final String SECTORS_URL = "https://smart-lab.ru/forum/sectors";
    private final Map<String, String> mapping = Stream.of(new String[][]{
            {"Металлургия черн.", "Металлургия"},
            {"Металлургия цвет.", "Металлургия"},
            {"Химия удобрения", "Химическая промышленность"},
            {"Химия разное", "Химическая промышленность"},
            {"Энергосбыт", "Электроэнергетика"},
            {"Ритейл", "Розничная торговля"},
            {"Интернет", "Информационные технологии"},
            {"Строители", "Девелопмент"},
            {"Драг.металлы", "Драгоценные металлы"},
            {"Горнодобывающие", "Полезные ископаемые"},
            {"Э/генерация", "Электроэнергетика"},
            {"Электросети", "Электроэнергетика"},
            {"Телеком", "Телекоммуникации"},
            {"Потреб", "Товары и услуги"},
            {"High tech", "Информационные технологии"},
            {"Financials", "Финансы"},
            {"Consumer discretionary", "Товары и услуги"},
            {"Consumer staples", "Розничная торговля"},
            {"Technology", "Информационные технологии"},
            {"Energy", "Энергоресурсы"},
            {"Industrials", "Промышленность"},
            {"Telecom", "Телекоммуникации"},
            {"Utilities", "Коммунальные услуги"},
            {"Real estate", "Девелопмент"},
            {"Healthcare", "Здравоохранение"},
            {"Materials", "Полезные ископаемые"},
            {"Etf", "ETF"}})
            .collect(Collectors.toMap(v -> v[0], v -> v[1]));

    /**
     * @ return sector -> share identity map. Most often, the identifier is a ticker, but this should be checked.
     */
    public Map<String, List<String>> getShareSectors() {
        try {
            Elements sectorElements = getHtmlDocument().select(".kompanii_sector");
            return sectorElements.stream()
                    .collect(groupingBy(this::getSectorName,
                            flatMapping(SmartlabShareSectors::getSmartlabShareIds, toList())));
        } catch (Exception e) {
            throw new RuntimeException("Произошла ошибка при получении списка секторов со Smart-Lab", e);
        }
    }

    private Document getHtmlDocument() throws IOException {
        return Jsoup.connect(SECTORS_URL)
                .method(Connection.Method.GET)
                .get();
    }

    private String getSectorName(Element sectorElement) {
        String sector = sectorElement.getElementsByTag("h2").get(0).text();
        sector = capitalize(sector.toLowerCase());
        return mapping.getOrDefault(sector, sector);
    }

    private static Stream<String> getSmartlabShareIds(Element sectorElement) {
        try {
            Elements shares = sectorElement.select("ul li a");
            return shares.stream()
                    .map(SmartlabShareSectors::convertToSmartlabShareId)
                    .filter(Objects::nonNull)
                    .map(String::toUpperCase);
        } catch (Exception e) {
            log.debug("Ошибка получения списка акций по сектору: {}", sectorElement, e);
        }
        return Stream.empty();
    }

    private static String convertToSmartlabShareId(Element shareElement) {
        try {
            String href = shareElement.attr("href");
            int pos = href.lastIndexOf('/');
            if (pos != -1) {
                return href.substring(pos + 1);
            }
        } catch (Exception e) {
            log.debug("Ошибка получения идентификатора акции: {}", shareElement, e);
        }
        return null;
    }
}

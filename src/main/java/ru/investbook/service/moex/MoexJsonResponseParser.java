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

package ru.investbook.service.moex;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

@Slf4j
public class MoexJsonResponseParser {

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> convertFromIntObjectMap(Map<?, ?> indicesResponse) {
        try {
            indicesResponse = (Map<?, ?>) indicesResponse.values().iterator().next();
            List<String> columnNames = (List<String>) indicesResponse.get("columns");
            Collection<List<Object>> dataObjects =
                    (Collection<List<Object>>) Optional.ofNullable(indicesResponse.get("data")).orElseThrow();
            List<Map<String, Object>> namedItems = new ArrayList<>(dataObjects.size());
            for (List<Object> obj : dataObjects) {
                HashMap<String, Object> namedObject = new HashMap<>();
                for (int i = 0, cnt = obj.size(); i < cnt; i++) {
                    namedObject.put(columnNames.get(i), obj.get(i));
                }
                namedItems.add(unmodifiableMap(namedObject));
            }
            return unmodifiableList(namedItems);
        } catch (Exception e) {
            log.info("Can't parse Moex ISS response: {}", indicesResponse, e);
            return null;
        }
    }
}

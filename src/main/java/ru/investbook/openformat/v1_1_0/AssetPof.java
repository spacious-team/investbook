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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.springframework.lang.Nullable;
import ru.investbook.entity.SecurityEntity;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
class AssetPof {

    @NotNull
    @JsonProperty("id")
    int id;

    @NotNull
    @JsonProperty("type")
    String type;

    @Nullable
    @JsonProperty("symbol")
    String symbol;

    @Nullable
    @JsonProperty("name")
    String name;

    @Nullable
    @JsonProperty("isin")
    String isin;


    static AssetPof of(SecurityEntity security) {
        return AssetPof.builder()
                .id(security.getId())
                .type(SecurityTypeHelper.toPofType(security.getType()))
                .symbol(security.getTicker())
                .name(security.getName())
                .isin(security.getIsin())
                .build();
    }

    Optional<Security> toSecurity() {
        try {
            return Optional.of(Security.builder()
                    .id(id)
                    .type(SecurityTypeHelper.getSecurityType(type))
                    .ticker(symbol)
                    .name(name)
                    .isin(isin)
                    .build());
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Optional.empty();
        }
    }
}

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

package ru.investbook.openformat.v1_1_0;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import ru.investbook.entity.SecurityEntity;

import java.util.Optional;

import static ru.investbook.openformat.OpenFormatHelper.getValidIsinOrNull;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
class AssetPof {

    @JsonProperty("id")
    @NotNull
    int id;

    @JsonProperty("type")
    @NotNull
    String type;

    @JsonProperty("symbol")
    @Nullable
    String symbol;

    @JsonProperty("name")
    @Nullable
    String name;

    @JsonProperty("isin")
    @Nullable
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
                    .type(getSecurityType())
                    .ticker(symbol)
                    .name(name)
                    .isin(getValidIsinOrNull(isin))
                    .build());
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Optional.empty();
        }
    }

    SecurityType getSecurityType() {
        return SecurityTypeHelper.getSecurityType(type);
    }
}

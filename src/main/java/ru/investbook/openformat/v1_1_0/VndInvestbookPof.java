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
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.SecurityDescription;
import org.spacious_team.broker.pojo.SecurityQuote;

import java.util.Collection;
import java.util.Collections;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VndInvestbookPof {

    @NotNull
    @Builder.Default
    @JsonProperty("version")
    String version;

    @NotNull
    @Builder.Default
    @JsonIgnoreProperties(value = {"id"})
    @JsonProperty("portfolio-cash")
    Collection<PortfolioCash> portfolioCash = Collections.emptySet();

    @NotNull
    @Builder.Default
    @JsonIgnoreProperties(value = {"id"})
    @JsonProperty("portfolio-properties")
    Collection<PortfolioProperty> portfolioProperties = Collections.emptySet();

    @NotNull
    @Builder.Default
    @JsonIgnoreProperties(value = {"issuer"})
    @JsonProperty("security-descriptions")
    Collection<SecurityDescription> securityDescriptions = Collections.emptySet();

    @NotNull
    @Builder.Default
    @JsonIgnoreProperties(value = {"id"})
    @JsonProperty("security-quotes")
    Collection<SecurityQuote> securityQuotes = Collections.emptySet();
}

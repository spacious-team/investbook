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

package db.migration.all;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;

@Slf4j
public class V2022_1_0_1 extends BaseJavaMigration {

    public void migrate(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT * FROM `portfolio_property` WHERE `property` = 'CASH'")) {
                ObjectMapper objectMapper = new ObjectMapper();
                while (rows.next()) {
                    String portfolio = rows.getString("portfolio");
                    Timestamp timestamp = rows.getTimestamp("timestamp");
                    String value = rows.getString("value");
                    Collection<PortfolioCash> cashCollection = PortfolioCash.deserialize(objectMapper, value);
                    try (Statement statement = context.getConnection().createStatement()) {
                        cashCollection.forEach(cash -> insertCash(statement, portfolio, timestamp, cash));
                    }
                }
            }
        }
        try (Statement delete = context.getConnection().createStatement()) {
            delete.execute("DELETE FROM `portfolio_property` WHERE `property` = 'CASH'");
        }
    }

    private void insertCash(Statement statement, String portfolio, Timestamp timestamp, PortfolioCash cash) {
        try {
            statement.execute("INSERT INTO `portfolio_cash` SET " +
                    "portfolio ='" + portfolio + "', " +
                    "timestamp = '" + timestamp + "', " +
                    "market = '" + cash.getSection() + "', " +
                    "value = " + cash.getValue() + ", " +
                    "currency = '" + cash.getCurrency() + "'");
        } catch (Exception e) {
            log.error("can't insert to table 'cash' value: portfolio = {}, timestamp = {}, cash = {}",
                    portfolio, timestamp, cash, e);
        }
    }


    @Getter
    @Setter
    private static class PortfolioCash {
        private String section;
        private BigDecimal value;
        private String currency;

        public static Collection<PortfolioCash> deserialize(ObjectMapper objectMapper, String value) {
            try {
                return objectMapper.readValue(value, new TypeReference<>() {
                });
            } catch (Exception e) {
                log.error("can't deserialize portfolio cash", e);
                return Collections.emptySet();
            }
        }
    }
}

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

package ru.investbook;

import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@Getter
@Setter
@Component
@ConfigurationProperties("investbook")
public class InvestbookProperties implements InitializingBean {

    private Path dataPath = Paths.get(System.getProperty("user.home", ""), "investbook");

    private Path reportBackupPath = dataPath.resolve("report-backups");

    private @MonotonicNonNull List<Path> sqlImportFiles;

    private boolean openHomePageAfterStart = false;

    private boolean tryAltIndexLogoUrl = true;

    /**
     * Configures extensions packages which provides TableFactory interfaces.
     * Do not configure {@link org.spacious_team.table_wrapper} package, because they are configured by default.
     */
    private Set<String> tableParsers = Set.of();

    private boolean reportBackup = true;

    public List<Path> getSqlImportFiles() {
        return requireNonNull(sqlImportFiles);
    }

    @Override
    @EnsuresNonNull("sqlImportFiles")
    public void afterPropertiesSet() {
        if (sqlImportFiles == null) {
            // lazy initialize. dataPath is configured at this point
            sqlImportFiles = List.of(
                    dataPath.resolve("export-2022.9.sql"),
                    dataPath.resolve("export-2024.1.x.sql"));
        }
    }
}

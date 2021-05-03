/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

@Component
@ConfigurationProperties("investbook")
@Getter
@Setter
public class InvestbookProperties {

    private Path reportBackupPath = Paths.get(System.getProperty("user.home", ""), "investbook", "report-backups");

    private boolean openHomePageAfterStart = false;

    /**
     * Configures extensions packages which provides TableFactory interfaces.
     * Do not configure {@link org.spacious_team.table_wrapper} package, because it configured by default.
     */
    private Collection<String> tableParsers = Collections.emptyList();

    private boolean reportBackup = true;
}

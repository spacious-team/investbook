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

package ru.investbook.repository;

import org.hibernate.exception.ConstraintViolationException;

import java.sql.SQLException;
import java.util.Objects;

public class RepositoryHelper {

    /**
     * May return false positive result if NOT NULL column set by NULL (or for other constraint violations)
     * for not H2 or MariaDB RDBMS
     */
    public static boolean isUniqIndexViolationException(Throwable t) {
        do {
            if (t instanceof ConstraintViolationException) {
                // todo Не точное условие, нужно выбирать
                Throwable cause = t.getCause();
                if (cause instanceof SQLException sqlException) {
                    int errorCode = sqlException.getErrorCode();
                    String sqlState = sqlException.getSQLState();
                    String packageName = cause.getClass().getPackageName();
                    // https://www.h2database.com/javadoc/org/h2/api/ErrorCode.html#DUPLICATE_KEY_1
                    if (errorCode == 23505 && Objects.equals(packageName, "org.h2.jdbc")) {
                        return true;  // H2
                    } else if (errorCode == 1062 &&
                            Objects.equals(sqlState, "23000") &&
                            Objects.equals(packageName, "java.sql")) {
                        // https://mariadb.com/kb/en/mariadb-error-code-reference/
                        return true;  // MariaDB
                    }
                }
                return true;  // other databases
            }
        } while ((t = t.getCause()) != null);
        return false;
    }
}

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

-- gh-223: fix failed by installer-2021.2 update 2021.2
EXECUTE IMMEDIATE NVL2(
    QUOTE_IDENT((SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'flyway_schema_history')),
    'DELETE IGNORE FROM "flyway_schema_history" WHERE "success" = 0 AND "version" = ''2021.2.0.0''',
    'CREATE ALIAS MY_SQRT FOR "java.lang.Math.sqrt"' -- do any noop command
);

-- Spring Boot since 2.5 doest not apply schema.sql + data.sql with flyway
-- Move schema.sql + data.sql statements to V2020_7_0_0.sql.
EXECUTE IMMEDIATE NVL2(
    QUOTE_IDENT((SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'flyway_schema_history')),
    'UPDATE "flyway_schema_history" SET "type" = ''SQL'', "script" = ''V1__init_schema.sql'', "description" = ''init schema'', "checksum" = 1917096800 WHERE "installed_rank" = 1',
    'CREATE ALIAS MY_SQRT2 FOR "java.lang.Math.sqrt"' -- do any noop command
);
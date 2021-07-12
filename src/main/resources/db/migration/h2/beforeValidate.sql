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
    'UPDATE "flyway_schema_history" SET "type" = ''SQL'', "script" = ''V1__init_schema.sql'', "description" = ''init schema'', "checksum" = 796963534 WHERE "version" = ''1''',
    'CREATE ALIAS MY_SQRT2 FOR "java.lang.Math.sqrt"' -- do any noop command
);

-- Fix failed db update from first installed 2021.5 to 2021.6
EXECUTE IMMEDIATE NVL2(
    QUOTE_IDENT((SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'flyway_schema_history')),
    'UPDATE "flyway_schema_history" SET "type" = ''BASELINE'', "description" = ''<< Flyway Baseline >>'', "script" = ''<< Flyway Baseline >>'', "version" = ''2021.5.0.0'', "checksum" = null  WHERE "version" = ''2021.5'' AND "installed_rank" = 1',
    'CREATE ALIAS MY_SQRT10 FOR "java.lang.Math.sqrt"' -- do any noop command
);

-- Update V2020_11_0_0.sql
EXECUTE IMMEDIATE NVL2(
    QUOTE_IDENT((SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'flyway_schema_history')),
    'UPDATE "flyway_schema_history" SET "checksum" = 1105894560 WHERE "version" = ''2020.11.0.0''',
    'CREATE ALIAS MY_SQRT3 FOR "java.lang.Math.sqrt"' -- do any noop command
);

-- Update V2020_13_0_0.sql
EXECUTE IMMEDIATE NVL2(
    QUOTE_IDENT((SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'flyway_schema_history')),
    'UPDATE "flyway_schema_history" SET "checksum" = 751495929 WHERE "version" = ''2020.13.0.0''',
    'CREATE ALIAS MY_SQRT4 FOR "java.lang.Math.sqrt"' -- do any noop command
);

-- Update V2020_14_0_0.sql
EXECUTE IMMEDIATE NVL2(
    QUOTE_IDENT((SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'flyway_schema_history')),
    'UPDATE "flyway_schema_history" SET "checksum" = -1589694755 WHERE "version" = ''2020.14.0.0''',
    'CREATE ALIAS MY_SQRT5 FOR "java.lang.Math.sqrt"' -- do any noop command
);

-- Update V2021_1_1_0.sql
EXECUTE IMMEDIATE NVL2(
    QUOTE_IDENT((SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'flyway_schema_history')),
    'UPDATE "flyway_schema_history" SET "checksum" = -1760445809 WHERE "version" = ''2021.1.1.0''',
    'CREATE ALIAS MY_SQRT6 FOR "java.lang.Math.sqrt"' -- do any noop command
);

-- gh-223: lets patching from 2021.1.1 to 2021.2
-- Update V2021_2_0_0.sql
EXECUTE IMMEDIATE NVL2(
    QUOTE_IDENT((SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'flyway_schema_history')),
    'UPDATE "flyway_schema_history" SET "checksum" = 1920695213 WHERE "version" = ''2021.2.0.0''',
    'CREATE ALIAS MY_SQRT8 FOR "java.lang.Math.sqrt"' -- do any noop command
);

-- Remove unnecessary data providing V2021_1_0_0.sql, V2021_2_0_1.sql
EXECUTE IMMEDIATE NVL2(
    QUOTE_IDENT((SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'flyway_schema_history')),
    'DELETE IGNORE FROM "flyway_schema_history" WHERE "version" IN (''2021.1.0.0'', ''2021.2.0.1'')',
    'CREATE ALIAS MY_SQRT9 FOR "java.lang.Math.sqrt"' -- do any noop command
);

-- Update V2021_5_0_0.sql
EXECUTE IMMEDIATE NVL2(
    QUOTE_IDENT((SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'flyway_schema_history')),
    'UPDATE "flyway_schema_history" SET "checksum" = 38693906 WHERE "version" = ''2021.5.0.0'' AND "installed_rank" <> 1',
    'CREATE ALIAS MY_SQRT10 FOR "java.lang.Math.sqrt"' -- do any noop command
);
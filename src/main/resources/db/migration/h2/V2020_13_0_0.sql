/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

EXECUTE IMMEDIATE NVL2(
    QUOTE_IDENT((SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'SECURITY' AND COLUMN_NAME = 'ID')),
    'CREATE ALIAS RANDOM_NAME1 FOR "java.lang.Math.sqrt"', -- do any noop command
    'ALTER TABLE "SECURITY" ALTER COLUMN IF EXISTS "ISIN" RENAME TO "ID"'
);
ALTER TABLE `security_event_cash_flow` ALTER COLUMN IF EXISTS `isin` RENAME TO `security`;
ALTER TABLE `security_quote` ALTER COLUMN IF EXISTS `isin` RENAME TO `security`;
ALTER TABLE `transaction` ALTER COLUMN IF EXISTS `isin` RENAME TO `security`;
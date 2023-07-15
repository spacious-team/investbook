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

-- do not change timestamp column value with UPDATE SET instruction
ALTER TABLE `event_cash_flow` CHANGE COLUMN `timestamp`
    `timestamp` TIMESTAMP NOT NULL DEFAULT current_timestamp();
ALTER TABLE `portfolio_property` CHANGE COLUMN `timestamp`
    `timestamp` TIMESTAMP NOT NULL DEFAULT current_timestamp() AFTER `portfolio`;
ALTER TABLE `security_event_cash_flow` CHANGE COLUMN `timestamp`
    `timestamp` TIMESTAMP NOT NULL DEFAULT current_timestamp() AFTER `portfolio`;
ALTER TABLE `security_quote` CHANGE COLUMN `timestamp`
    `timestamp` TIMESTAMP NOT NULL DEFAULT current_timestamp() COMMENT 'Время котировки';
ALTER TABLE `transaction` CHANGE COLUMN `timestamp`
    `timestamp` TIMESTAMP NOT NULL DEFAULT current_timestamp() COMMENT 'Фактическое время исполнения сделки';

-- V2021_11_0_3.sql requirement
ALTER TABLE `security` CHANGE COLUMN `ticker` `ticker` VARCHAR(32) NULL DEFAULT NULL COMMENT 'Тикер';
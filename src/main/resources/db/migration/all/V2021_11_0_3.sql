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

-- drop indexes and foreigh keys
ALTER TABLE `security_description` DROP FOREIGN KEY `security_description_security_fkey`;
ALTER TABLE `security_description` DROP PRIMARY KEY;

ALTER TABLE `security_event_cash_flow` DROP FOREIGN KEY `security_event_cash_flow_security_fkey`;
ALTER TABLE `security_event_cash_flow` DROP INDEX `security_event_cash_flow_security_ix`;
ALTER TABLE `security_event_cash_flow` DROP INDEX `security_event_cash_flow_timestamp_security_type_portfolio_uniq`;

ALTER TABLE `security_quote` DROP FOREIGN KEY `security_quote_security_fkey`;
ALTER TABLE `security_quote` DROP INDEX `security_quote_security_ix`;
ALTER TABLE `security_quote` DROP INDEX `security_quote_security_timestamp_uniq_ix`;

ALTER TABLE `transaction` DROP FOREIGN KEY `transaction_security_fkey`;
ALTER TABLE `transaction` DROP INDEX `transaction_security_ix`;

ALTER TABLE `security` DROP PRIMARY KEY;

-- add INT security_id column, it will be renamed and set to NOT NULL by V2021_11_0_4.sql
ALTER TABLE `security` ADD COLUMN `security_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE `security_description` ADD COLUMN `security_id` INT(10) UNSIGNED COMMENT 'Идентификатор ценной бумаги' FIRST;
ALTER TABLE `security_event_cash_flow` ADD COLUMN `security_id` INT(10) UNSIGNED COMMENT 'Инструмент, по которому произошло событие' AFTER `security`;
ALTER TABLE `security_quote` ADD COLUMN `security_id` INT(10) UNSIGNED COMMENT 'Инструмент (акция, облигация, контракт)' AFTER `security`;
ALTER TABLE `transaction` ADD COLUMN `security_id` INT(10) UNSIGNED COMMENT 'Инструмент (акция, облигация, контракт)' AFTER `security`;

-- set security_id column values
UPDATE `security_description` t
    SET t.security_id = (SELECT s.security_id FROM `security` s WHERE t.security = s.id);
UPDATE `security_event_cash_flow` t
    SET t.security_id = (SELECT s.security_id FROM `security` s WHERE t.security = s.id);
UPDATE `security_quote` t
    SET t.security_id = (SELECT s.security_id FROM `security` s WHERE t.security = s.id);
UPDATE `transaction` t
    SET t.security_id = (SELECT s.security_id FROM `security` s WHERE t.security = s.id);

-- add indexes and foreign keys
-- -- `security_description` table primary key should be NOT NULL, PRIMARY and FOREIGN KEY will be added by V2021_11_0_4.sql

ALTER TABLE `security_event_cash_flow` ADD KEY `security_event_cash_flow_security_ix` (`security_id`);
ALTER TABLE `security_event_cash_flow`
    ADD UNIQUE KEY `security_event_cash_flow_timestamp_security_type_portfolio_uniq` (`timestamp`,`security_id`,`type`,`portfolio`);
ALTER TABLE `security_event_cash_flow` ADD CONSTRAINT `security_event_cash_flow_security_fkey`
    FOREIGN KEY (`security_id`) REFERENCES `security` (`security_id`) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `security_quote` ADD KEY `security_quote_security_ix` (`security_id`);
ALTER TABLE `security_quote`
    ADD UNIQUE KEY `security_quote_security_timestamp_uniq_ix` (`security_id`, `timestamp`);
ALTER TABLE `security_quote` ADD CONSTRAINT `security_quote_security_fkey`
    FOREIGN KEY (`security_id`) REFERENCES `security` (`security_id`) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `transaction` ADD KEY `transaction_security_ix` (`security_id`);
ALTER TABLE `transaction` ADD CONSTRAINT `transaction_security_fkey`
    FOREIGN KEY (`security_id`) REFERENCES `security` (`security_id`) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `security` ADD UNIQUE KEY `security_isin_uniq_ix` (`isin`);
ALTER TABLE `security` ADD UNIQUE KEY `security_ticker_uniq_ix` (`ticker`);
ALTER TABLE `security` ADD UNIQUE KEY `security_name_uniq_ix` (`name`); -- for uniq asset

-- preserve ISIN and contract codes
UPDATE `security` SET `isin` = `id` WHERE `type` IN (0, 1, 2);
UPDATE `security` SET `ticker` = `id` WHERE `type` IN (3, 4);

-- drop old security columns
ALTER TABLE `security` DROP COLUMN `id`;
ALTER TABLE `security_description` DROP COLUMN `security`;
ALTER TABLE `security_event_cash_flow` DROP COLUMN `security`;
ALTER TABLE `security_quote` DROP COLUMN `security`;
ALTER TABLE `transaction` DROP COLUMN `security`;

-- to be continued in V2021_11_0_4.sql

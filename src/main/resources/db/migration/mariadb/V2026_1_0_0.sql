/*
 * InvestBook
 * Copyright (C) 2026  Spacious Team <spacious-team@ya.ru>
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

-- Renames `portfolio` field to `account` in tables, renames indices and foreign keys

-- account
ALTER TABLE `portfolio` RENAME TO `account`;
ALTER TABLE `account`
    CHANGE COLUMN `id` `id` VARCHAR(32) NOT NULL COMMENT 'Номер счета';

-- account_cash
ALTER TABLE `portfolio_cash` RENAME TO `account_cash`;
ALTER TABLE `account_cash`
    CHANGE COLUMN `portfolio` `account` VARCHAR(32) NOT NULL COMMENT 'Номер счета',
    DROP INDEX `portfolio_cash_portfolio_market_timestamp_currency_uniq_ix`,
    ADD UNIQUE INDEX `account_cash_account_market_timestamp_currency_uniq_ix` (`account`, `market`, `timestamp`, `currency`),
    DROP INDEX `portfolio_cash_portfolio_ix`,
    ADD INDEX `account_cash_account_ix` (`account`),
    DROP FOREIGN KEY `portfolio_cash_portfolio_fkey`,
    ADD CONSTRAINT `account_cash_account_fkey` FOREIGN KEY (`account`) REFERENCES `account` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;

-- account_property
ALTER TABLE `portfolio_property` RENAME TO `account_property`;
ALTER TABLE `account_property`
    CHANGE COLUMN `portfolio` `account` VARCHAR(32) NOT NULL COMMENT 'Номер счета',
    DROP INDEX `portfolio_property_portfolio_timestamp_property_uniq_ix`,
    ADD UNIQUE INDEX `account_property_account_timestamp_property_uniq_ix` (`account`, `timestamp`, `property`),
    DROP INDEX `portfolio_property_portfolio_ix`,
    ADD INDEX `account_property_account_ix` (`account`),
    DROP FOREIGN KEY `portfolio_property_portfolio_fkey`,
    ADD CONSTRAINT `account_property_account_fkey` FOREIGN KEY (`account`) REFERENCES `account` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;

-- event_cash_flow
ALTER TABLE `event_cash_flow`
    CHANGE COLUMN `portfolio` `account` VARCHAR(32) NOT NULL COMMENT 'Номер счета',
    DROP INDEX `event_cash_flow_timestamp_type_value_currency_portfolio_uniq_ix`,
    ADD UNIQUE INDEX `event_cash_flow_timestamp_type_value_currency_account_uniq_ix` (`timestamp`, `type`, `value`, `currency`, `account`),
    DROP INDEX `event_cash_flow_portfolio_ix`,
    ADD INDEX `event_cash_flow_account_ix` (`account`),
    DROP FOREIGN KEY `event_cash_flow_portfolio_fkey`,
    ADD CONSTRAINT `event_cash_flow_account_fkey` FOREIGN KEY (`account`) REFERENCES `account` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;

-- security_event_cash_flow
ALTER TABLE `security_event_cash_flow`
    CHANGE COLUMN `portfolio` `account` VARCHAR(32) NOT NULL COMMENT 'Номер счета',
    DROP INDEX `security_event_cash_flow_timestamp_security_type_portfolio_uniq`,
    ADD UNIQUE INDEX `security_event_cash_flow_timestamp_security_type_account_uniq_ix` (`timestamp`, `security`, `type`, `account`),
    DROP INDEX `security_event_cash_flow_portfolio_ix`,
    ADD INDEX `security_event_cash_flow_account_ix` (`account`),
    DROP FOREIGN KEY `security_event_cash_flow_portfolio_fkey`,
    ADD CONSTRAINT `security_event_cash_flow_account_fkey` FOREIGN KEY (`account`) REFERENCES `account` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;

-- transaction
ALTER TABLE `transaction`
    CHANGE COLUMN `portfolio` `account` VARCHAR(32) NOT NULL COMMENT 'Номер счета',
    DROP INDEX `transaction_trade_id_portfolio_uniq`,
    ADD UNIQUE INDEX `transaction_trade_id_account_uniq_ix` (`trade_id`, `account`) ,
    DROP INDEX `transaction_portfolio_ix`,
    ADD INDEX `transaction_account_ix` (`account`),
    DROP FOREIGN KEY `transaction_portfolio_fkey`,
    ADD CONSTRAINT `transaction_account_fkey` FOREIGN KEY (`account`) REFERENCES `account` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;

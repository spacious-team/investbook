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
ALTER TABLE `account` CHANGE COLUMN `id` `id` VARCHAR(32) NOT NULL COMMENT 'Номер счета';

-- account_cash
ALTER TABLE `portfolio_cash` RENAME TO `account_cash`;
ALTER TABLE `account_cash` CHANGE COLUMN `portfolio` `account` VARCHAR(32) NOT NULL COMMENT 'Номер счета';
ALTER INDEX `portfolio_cash_portfolio_ix` RENAME TO `account_cash_account_ix`;
ALTER TABLE `account_cash` RENAME CONSTRAINT `portfolio_cash_portfolio_market_timestamp_currency_uniq_ix`
    TO `account_cash_account_market_timestamp_currency_uniq_ix`;
ALTER TABLE `account_cash` RENAME CONSTRAINT `portfolio_cash_portfolio_fkey` TO `account_cash_account_fkey`;

-- account_property
ALTER TABLE `portfolio_property` RENAME TO `account_property`;
ALTER TABLE `account_property` CHANGE COLUMN `portfolio` `account` VARCHAR(32) NOT NULL COMMENT 'Номер счета';
ALTER INDEX `portfolio_property_portfolio_ix` RENAME TO `account_property_account_ix`;
ALTER TABLE `account_property` RENAME CONSTRAINT `portfolio_property_portfolio_timestamp_property_uniq_ix`
    TO `account_property_account_timestamp_property_uniq_ix`;
ALTER TABLE `account_property` RENAME CONSTRAINT `portfolio_property_portfolio_fkey` TO `account_property_account_fkey`;

-- event_cash_flow
ALTER TABLE `event_cash_flow` CHANGE COLUMN `portfolio` `account` VARCHAR(32) NOT NULL COMMENT 'Номер счета';
ALTER INDEX `event_cash_flow_portfolio_ix` RENAME TO `event_cash_flow_account_ix`;
ALTER TABLE `event_cash_flow` RENAME CONSTRAINT `event_cash_flow_timestamp_type_value_currency_portfolio_uniq_ix`
    TO `event_cash_flow_timestamp_type_value_currency_account_uniq_ix`;
ALTER TABLE `event_cash_flow` RENAME CONSTRAINT `event_cash_flow_portfolio_fkey` TO `event_cash_flow_account_fkey`;

-- security_event_cash_flow
ALTER TABLE `security_event_cash_flow` CHANGE COLUMN `portfolio` `account` VARCHAR(32) NOT NULL COMMENT 'Номер счета';
ALTER INDEX `security_event_cash_flow_portfolio_ix` RENAME TO `security_event_cash_flow_account_ix`;
ALTER TABLE `security_event_cash_flow` RENAME CONSTRAINT `security_event_cash_flow_timestamp_security_type_portfolio_uniq`
    TO `security_event_cash_flow_timestamp_security_type_account_uniq_ix`;
ALTER TABLE `security_event_cash_flow` RENAME CONSTRAINT `security_event_cash_flow_portfolio_fkey` TO `security_event_cash_flow_account_fkey`;

-- transaction
ALTER TABLE `transaction` CHANGE COLUMN `portfolio` `account` VARCHAR(32) NOT NULL COMMENT 'Номер счета';
ALTER INDEX `transaction_portfolio_ix` RENAME TO `transaction_account_ix`;
ALTER TABLE `transaction` RENAME CONSTRAINT `transaction_trade_id_portfolio_uniq`
    TO `transaction_trade_id_account_uniq_ix`;
ALTER TABLE `transaction` RENAME CONSTRAINT `transaction_portfolio_fkey` TO `transaction_account_fkey`;

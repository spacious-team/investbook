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

ALTER TABLE `transaction_cash_flow` DROP FOREIGN KEY `transaction_cash_flow_transaction_id_fkey`;
ALTER TABLE `transaction_cash_flow` DROP FOREIGN KEY `transaction_cash_flow_portfolio_fkey`;
ALTER TABLE `transaction_cash_flow` DROP KEY `transaction_cash_flow_transaction_id_ix`;
ALTER TABLE `transaction_cash_flow` DROP KEY `transaction_cash_flow_portfolio_ix`;
ALTER TABLE `transaction_cash_flow` DROP PRIMARY KEY;

ALTER TABLE `transaction` DROP PRIMARY KEY;
ALTER TABLE `transaction` CHANGE COLUMN `id` `trade_id` varchar(32) NOT NULL COMMENT 'Номер сделки в системе учета брокера';
ALTER TABLE `transaction` ADD UNIQUE KEY `transaction_trade_id_portfolio_uniq` (`trade_id`, `portfolio`);
ALTER TABLE `transaction` ADD COLUMN `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

ALTER TABLE `transaction_cash_flow` CHANGE COLUMN `transaction_id` `trade_id` varchar(32) NOT NULL COMMENT 'ID транзакции';
ALTER TABLE `transaction_cash_flow` ADD COLUMN `transaction_id` int(10) UNSIGNED NOT NULL FIRST;
UPDATE `transaction_cash_flow` c JOIN `transaction` t
    ON t.trade_id = c.trade_id AND t.portfolio = c.portfolio
    SET c.transaction_id = t.id;

ALTER TABLE `transaction_cash_flow` ADD KEY `transaction_cash_flow_transaction_id_ix` (`transaction_id`);
ALTER TABLE `transaction_cash_flow` ADD CONSTRAINT `transaction_cash_flow_transaction_id_fkey`
    FOREIGN KEY (`transaction_id`) REFERENCES `transaction` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `transaction_cash_flow` ADD COLUMN `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE `transaction_cash_flow` DROP COLUMN `trade_id`;
ALTER TABLE `transaction_cash_flow` DROP COLUMN `portfolio`;
ALTER TABLE `transaction_cash_flow` ADD UNIQUE KEY `transaction_cash_flow_transaction_id_type_uniq` (`transaction_id`, `type`);

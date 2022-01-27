/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

-- UNIQ index requires NOT NULL column
UPDATE `portfolio_cash` SET `market` = '' WHERE `market` IS NULL;
ALTER TABLE `portfolio_cash` CHANGE COLUMN `market`
    `market` VARCHAR(16) NOT NULL DEFAULT '' COMMENT 'Рынок: фондовый, срочный, валютный и т.п.';

ALTER TABLE `portfolio_cash`
    ADD KEY `portfolio_cash_portfolio_ix` (`portfolio`),
    DROP KEY `cash_portfolio_ix`;

ALTER TABLE `portfolio_cash`
    ADD UNIQUE KEY `portfolio_cash_portfolio_market_timestamp_currency_uniq_ix`(`portfolio`, `market`, `timestamp`, `currency`),
    DROP KEY `cash_portfolio_market_timestamp_currency_uniq_ix`;

ALTER TABLE `portfolio_cash`
    ADD CONSTRAINT `portfolio_cash_portfolio_fkey` FOREIGN KEY (`portfolio`)
        REFERENCES `portfolio` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,
    DROP FOREIGN KEY `cash_portfolio_fkey`;

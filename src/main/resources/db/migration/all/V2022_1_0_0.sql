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

CREATE TABLE `portfolio_cash` (
    `id`        INT(10)        NOT NULL AUTO_INCREMENT,
    `portfolio` VARCHAR(32)    NOT NULL COMMENT 'Идентификатор портфеля',
    `market` VARCHAR(16) NULL DEFAULT NULL COMMENT 'Рынок: фондовый, срочный, валютный и т.п.',
    `timestamp` TIMESTAMP      NOT NULL DEFAULT current_timestamp(),
    `value`     DECIMAL(12, 2) NOT NULL,
    `currency`  CHAR(3)        NOT NULL,
    PRIMARY KEY (`id`),
    KEY `cash_portfolio_ix` (`portfolio`),
    UNIQUE INDEX `cash_portfolio_market_timestamp_currency_uniq_ix` (`portfolio`, `market`, `timestamp`, `currency`),
    CONSTRAINT `cash_portfolio_fkey` FOREIGN KEY (`portfolio`) REFERENCES `portfolio` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Остаток денежных средств на счете';

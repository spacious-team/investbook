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

ALTER TABLE `security`
    CHANGE COLUMN `isin` `id` VARCHAR(64) NOT NULL
        COMMENT 'Идентификатор ценной бумаги: ISIN - для акций, облигаций; наименование контракта - для срочного и валютного рынка'
        FIRST;


ALTER TABLE `security_event_cash_flow`
    CHANGE COLUMN `isin` `security` varchar(64) NOT NULL
        COMMENT 'Инструмент, по которому произошло событие'
        AFTER `timestamp`;
ALTER TABLE `security_event_cash_flow`
    DROP KEY `security_event_cash_flow_timestamp_isin_type_portfolio_uniq_ix`,
    ADD UNIQUE KEY `security_event_cash_flow_timestamp_security_type_portfolio_uniq` (`timestamp`, `security`, `type`, `portfolio`),
    DROP FOREIGN KEY `security_event_cash_flow_isin_fkey`,
    ADD CONSTRAINT `security_event_cash_flow_security_fkey` FOREIGN KEY (`security`) REFERENCES `security` (`id`) ON UPDATE CASCADE;


ALTER TABLE `security_quote`
    CHANGE COLUMN `isin` `security` varchar(64) NOT NULL
        COMMENT 'Инструмент (акция, облигация, контракт)'
        AFTER `id`;
ALTER TABLE `security_quote`
    DROP KEY `security_quote_isin_timestamp_uniq_ix`,
    ADD UNIQUE KEY `security_quote_security_timestamp_uniq_ix` (`security`, `timestamp`),
    DROP KEY `security_quote_isin_fkey`,
    ADD KEY `security_quote_security_fkey` (`security`),
    DROP FOREIGN KEY `security_quote_isin_fkey`,
    ADD CONSTRAINT `security_quote_security_fkey` FOREIGN KEY (`security`) REFERENCES `security` (`id`) ON UPDATE CASCADE;


ALTER TABLE `transaction`
    CHANGE COLUMN `isin` `security` VARCHAR(64) NOT NULL
        COMMENT 'Инструмент (акция, облигация, контракт)'
        AFTER `portfolio`;
ALTER TABLE `transaction`
    DROP INDEX `transaction_ticker_ix`,
    ADD INDEX `transaction_security_ix` (`security`),
    DROP FOREIGN KEY `transaction_isin_fkey`,
    ADD CONSTRAINT `transaction_security_fkey` FOREIGN KEY (`security`) REFERENCES `security` (`id`) ON UPDATE CASCADE;
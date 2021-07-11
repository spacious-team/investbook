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

-- Дамп структуры для таблица portfolio.security_description
-- Нужно проверить наличие issuer.id, может отсутствовать для БД < 2021.5. На колонку создается ссылка.
ALTER TABLE `security` DROP CONSTRAINT IF EXISTS `security_issuer_inn_fkey`;
ALTER TABLE `issuer` DROP COLUMN IF EXISTS `inn`;
ALTER TABLE `issuer` ADD COLUMN IF NOT EXISTS `id` int(11) UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT FIRST;
CREATE TABLE IF NOT EXISTS `security_description` (
    `security` varchar(64) NOT NULL COMMENT 'Идентификатор ценной бумаги',
    `sector` varchar(32) DEFAULT NULL COMMENT 'Сектор экономики (применимо только для акций)',
    `issuer` int(10) unsigned DEFAULT NULL COMMENT 'Эмитент',
    PRIMARY KEY (`security`),
    KEY `security_description_issuer_ix` (`issuer`),
    CONSTRAINT `security_description_security_fkey` FOREIGN KEY (`security`) REFERENCES `security` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT `security_description_issuer_fkey` FOREIGN KEY (`issuer`) REFERENCES `issuer` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Расширенная информация по ценным бумагам';

-- rename wrong named indices
ALTER INDEX IF EXISTS
	`security_event_cash_flow_ticker_ix` RENAME TO `security_event_cash_flow_security_ix`;

ALTER INDEX IF EXISTS
	`security_quote_security_fkey` RENAME TO `security_quote_security_ix`;

ALTER INDEX IF EXISTS
	`transaction_cash_flow_type_key` RENAME TO `transaction_cash_flow_type_ix`;

-- change schema to 2021.5
ALTER TABLE `security`
    DROP CONSTRAINT IF EXISTS `security_issuer_inn_ix`;
ALTER TABLE `security`
    DROP CONSTRAINT IF EXISTS `security_issuer_inn_fkey`;
ALTER TABLE `security`
    DROP COLUMN IF EXISTS `issuer_inn`;
ALTER TABLE `security`
    ADD COLUMN IF NOT EXISTS `isin` char(12) DEFAULT NULL COMMENT 'ISIN' AFTER id;

ALTER TABLE `issuer`
    DROP COLUMN IF EXISTS `inn`;
ALTER TABLE `issuer`
    ADD COLUMN IF NOT EXISTS `id` int(11) UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT FIRST;
ALTER TABLE `issuer`
    ADD COLUMN IF NOT EXISTS
    `taxpayer_id` varchar(16) DEFAULT NULL COMMENT 'Идентификатор налогоплательщика (Россия - ИНН, США - EIN и т.д.)'
    AFTER `id`;

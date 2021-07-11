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

-- Был добавлен в 2020.10, включен сюда, чтобы не создавать старый патч-файл
CREATE TABLE IF NOT EXISTS `security_quote` (
    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `security` varchar(64) NOT NULL COMMENT 'Инструмент (акция, облигация, контракт)',
    `timestamp` timestamp NOT NULL COMMENT 'Время котировки',
    `quote` decimal(12,6) NOT NULL COMMENT 'Котировка акции в валюте/ дериватива - пунктах / облигации - в процентах / валютной пары - в валюте котируемой валюты',
    `price` decimal(12,6) DEFAULT NULL COMMENT 'Чистая цена облигации в валюте / стоимость срочных контрактов в валюте / для акций и валютной пары не заполняется',
    `accrued_interest` decimal(12,6) DEFAULT NULL COMMENT 'НКД для облигаций',
    `currency` char(3) DEFAULT NULL COMMENT 'Код валюты (опционально)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `security_quote_security_timestamp_uniq_ix` (`security`, `timestamp`),
    KEY `security_quote_security_ix` (`security`),
    CONSTRAINT `security_quote_security_fkey` FOREIGN KEY (`security`) REFERENCES `security` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Котировка (цена) финансовых инструментов';

ALTER TABLE `portfolio_property`
    ALTER COLUMN `value` VARCHAR(1024) NOT NULL;
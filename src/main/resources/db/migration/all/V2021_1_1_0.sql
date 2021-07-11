/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

-- Дамп структуры для таблица portfolio.foreign_exchange_rate
CREATE TABLE IF NOT EXISTS `foreign_exchange_rate` (
   `date` DATE NOT NULL,
   `currency_pair` CHAR(6) NOT NULL COMMENT 'Валютная пара (например USDRUB, базовая USD и котировальная валюты RUB)',
    `rate` DECIMAL(7,4) NOT NULL COMMENT 'Официальный обменный курс ЦБ',
    PRIMARY KEY (`currency_pair`, `date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='Официальные обменные курсы валют';

INSERT IGNORE INTO `foreign_exchange_rate` (`date`, `currency_pair`, `rate`)
    SELECT DISTINCT `timestamp`, substr(`property`, 0, 6), `value`
    FROM PORTFOLIO_PROPERTY where `property` LIKE '______\_EXCHANGE\_RATE';

DELETE FROM `portfolio_property`
    WHERE `property` LIKE '______\_EXCHANGE\_RATE';

UPDATE `portfolio_property`
    SET `property` = 'TOTAL_ASSETS_RUB', `timestamp` = `timestamp`
    WHERE `property` = 'TOTAL_ASSETS';

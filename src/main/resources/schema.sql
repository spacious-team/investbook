/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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

-- --------------------------------------------------------
-- Хост:                         127.0.0.1
-- Версия сервера:               10.4.12-MariaDB - mariadb.org binary distribution
-- Операционная система:         Win64
-- HeidiSQL Версия:              10.2.0.5599
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


-- Дамп структуры базы данных portfolio
-- CREATE DATABASE IF NOT EXISTS `portfolio` /*!40100 DEFAULT CHARACTER SET utf8 */;
-- USE `portfolio`;

-- Дамп структуры для таблица portfolio.cash_flow_type
CREATE TABLE IF NOT EXISTS `cash_flow_type` (
  `id` int(10) unsigned NOT NULL,
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Тип движения средств';

-- Дамп структуры для таблица portfolio.portfolio
CREATE TABLE IF NOT EXISTS `portfolio` (
  `id` varchar(32) NOT NULL COMMENT 'Портфель (номер брокерского счета)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Таблица пользователей';

-- Экспортируемые данные не выделены.

-- Дамп структуры для таблица portfolio.event_cash_flow
CREATE TABLE IF NOT EXISTS `event_cash_flow` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `portfolio` varchar(32) NOT NULL COMMENT 'Портфель (номер брокерского счета)',
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `type` int(10) unsigned NOT NULL COMMENT 'Причина движения',
  `value` decimal(12,2) NOT NULL COMMENT 'Размер',
  `currency` char(3) NOT NULL DEFAULT 'RUR' COMMENT 'Код валюты',
  `description` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `event_cash_flow_timestamp_type_value_currency_portfolio_uniq_ix` (`timestamp`,`type`,`value`,`currency`,`portfolio`),
  KEY `event_cash_flow_type_ix` (`type`),
  KEY `event_cash_flow_portfolio_ix` (`portfolio`),
  CONSTRAINT `event_cash_flow_portfolio_fkey` FOREIGN KEY (`portfolio`) REFERENCES `portfolio` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `event_cash_flow_type_fkey` FOREIGN KEY (`type`) REFERENCES `cash_flow_type` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Движение денежных средств, не связанное с ЦБ';

-- Дамп структуры для таблица portfolio.issuer
CREATE TABLE IF NOT EXISTS `issuer` (
  `inn` bigint(10) unsigned NOT NULL COMMENT 'ИНН',
  `name` varchar(100) NOT NULL COMMENT 'Наименование',
  PRIMARY KEY (`inn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Эмитенты';

-- Дамп структуры для таблица portfolio.portfolio_property
CREATE TABLE IF NOT EXISTS `portfolio_property` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `portfolio` varchar(32) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `property` varchar(64) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `portfolio_property_portfolio_timestamp_property_uniq_ix` (`portfolio`,`timestamp`,`property`),
  KEY `portfolio_property_portfolio_ix` (`portfolio`),
  CONSTRAINT `portfolio_property_portfolio_fkey` FOREIGN KEY (`portfolio`) REFERENCES `portfolio` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Свойства портфеля';

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

-- Экспортируемые данные не выделены.

-- Дамп структуры для таблица portfolio.security
CREATE TABLE IF NOT EXISTS `security` (
  `id` varchar(64) NOT NULL COMMENT 'Идентификатор ценной бумаги: ISIN - для акций, облигаций; наименование контракта - для срочного и валютного рынка',
  `ticker` varchar(16) DEFAULT NULL COMMENT 'Тикер',
  `name` varchar(100) DEFAULT NULL COMMENT 'Полное наименование ценной бумаги или контракта',
  `issuer_inn` bigint(10) unsigned DEFAULT NULL COMMENT 'Эмитент (ИНН)',
  PRIMARY KEY (`id`),
  KEY `security_issuer_inn_ix` (`issuer_inn`),
  CONSTRAINT `security_issuer_inn_fkey` FOREIGN KEY (`issuer_inn`) REFERENCES `issuer` (`inn`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Общая информация по ценным бумагам';

-- Дамп структуры для таблица portfolio.security_description
CREATE TABLE IF NOT EXISTS `security_description` (
    `security` varchar(64) NOT NULL COMMENT 'Идентификатор ценной бумаги',
    `sector` varchar(32) DEFAULT NULL COMMENT 'Сектор экономики (применимо только для акций)',
    PRIMARY KEY (`security`),
    CONSTRAINT `security_description_security_fkey` FOREIGN KEY (`security`) REFERENCES `security` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Расширенная информация по ценным бумагам';

-- Дамп структуры для таблица portfolio.security_event_cash_flow
CREATE TABLE IF NOT EXISTS `security_event_cash_flow` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `portfolio` varchar(32) NOT NULL COMMENT 'Портфель (номер брокерского счета)',
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `security` varchar(64) NOT NULL COMMENT 'Инструмент, по которому произошло событие',
  `count` int(1) unsigned NOT NULL COMMENT 'Количество ЦБ, по которым произошло событие (для деривативов - опциональное поле, можно заполнять 0)',
  `type` int(10) unsigned NOT NULL COMMENT 'Причина движения',
  `value` decimal(12,2) NOT NULL COMMENT 'Размер',
  `currency` char(3) NOT NULL DEFAULT 'RUR' COMMENT 'Код валюты',
  PRIMARY KEY (`id`),
  UNIQUE KEY `security_event_cash_flow_timestamp_security_type_portfolio_uniq` (`timestamp`,`security`,`type`,`portfolio`),
  KEY `security_event_cash_flow_type_ix` (`type`),
  KEY `security_event_cash_flow_security_ix` (`security`),
  KEY `security_event_cash_flow_portfolio_ix` (`portfolio`),
  CONSTRAINT `security_event_cash_flow_security_fkey` FOREIGN KEY (`security`) REFERENCES `security` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `security_event_cash_flow_portfolio_fkey` FOREIGN KEY (`portfolio`) REFERENCES `portfolio` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `security_event_cash_flow_type_fkey` FOREIGN KEY (`type`) REFERENCES `cash_flow_type` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Движение денежных средств, связанное с ЦБ';

-- Дамп структуры для таблица portfolio.security_quote
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

-- Дамп структуры для таблица portfolio.transaction
CREATE TABLE IF NOT EXISTS `transaction` (
  `id` varchar(32) NOT NULL COMMENT 'Номер сделки в системе учета брокера',
  `portfolio` varchar(32) NOT NULL COMMENT 'Портфель (номер брокерского счета)',
  `security` varchar(64) NOT NULL COMMENT 'Инструмент (акция, облигация, контракт)',
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'Фактическое время исполнения сделки',
  `count` int(1) NOT NULL COMMENT 'Покупка (+), продажа (-)',
  PRIMARY KEY (`id`,`portfolio`),
  KEY `transaction_security_ix` (`security`),
  KEY `transaction_portfolio_ix` (`portfolio`),
  CONSTRAINT `transaction_security_fkey` FOREIGN KEY (`security`) REFERENCES `security` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `transaction_portfolio_fkey` FOREIGN KEY (`portfolio`) REFERENCES `portfolio` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Сделки';

-- Дамп структуры для таблица portfolio.transaction_cash_flow
CREATE TABLE IF NOT EXISTS `transaction_cash_flow` (
  `transaction_id` varchar(32) NOT NULL COMMENT 'ID транзакции',
  `portfolio` varchar(32) NOT NULL COMMENT 'ID портфеля',
  `type` int(10) unsigned NOT NULL COMMENT 'Причина движения',
  `value` decimal(12,2) NOT NULL COMMENT 'Размер',
  `currency` char(3) NOT NULL DEFAULT 'RUR' COMMENT 'Код валюты',
  PRIMARY KEY (`transaction_id`,`portfolio`,`type`),
  KEY `transaction_cash_flow_type_ix` (`type`),
  KEY `transaction_cash_flow_transaction_id_ix` (`transaction_id`),
  KEY `transaction_cash_flow_portfolio_ix` (`portfolio`),
  CONSTRAINT `transaction_cash_flow_portfolio_fkey` FOREIGN KEY (`portfolio`) REFERENCES `portfolio` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `transaction_cash_flow_transaction_id_fkey` FOREIGN KEY (`transaction_id`) REFERENCES `transaction` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `transaction_cash_flow_type_fkey` FOREIGN KEY (`type`) REFERENCES `cash_flow_type` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='Движение денежных средств';

-- Дамп структуры для таблица portfolio.stock_market_index
CREATE TABLE IF NOT EXISTS `stock_market_index` (
  `date` DATE NOT NULL,
  `sp500` DECIMAL(7,2) NULL DEFAULT NULL COMMENT 'Значение индекса S&P 500',
  PRIMARY KEY (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='Индексы фондовых рынков';

-- Дамп структуры для таблица portfolio.foreign_exchange_rate
CREATE TABLE IF NOT EXISTS `foreign_exchange_rate` (
  `date` DATE NOT NULL,
  `currency_pair` CHAR(6) NOT NULL COMMENT 'Валютная пара (например USDRUB, базовая USD и котировальная валюты RUB)',
  `rate` DECIMAL(7,4) NOT NULL COMMENT 'Официальный обменный курс ЦБ',
  PRIMARY KEY (`currency_pair`, `date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='Официальные обменные курсы валют';

/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;

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

CREATE TABLE IF NOT EXISTS `security_type` (
    `id`   int(2) UNSIGNED NOT NULL,
    `name` varchar(50) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Тип ценных бумаг';

INSERT INTO `security_type` (`id`, `name`)
VALUES (0, 'Акция, ETF'),
       (1, 'Облигация'),
       (2, 'Акция или облигация'),
       (3, 'Срочный контракт'),
       (4, 'Валютная пара'),
       (5, 'Произвольный актив');

ALTER TABLE `security`
    ADD COLUMN `type` INT(2) UNSIGNED COMMENT 'Тип ценной бумаги' AFTER `id`;

UPDATE `security` SET `type` = 2 WHERE length(id) = 12;
UPDATE `security` SET `type` = 4 WHERE id LIKE '______\_%';
UPDATE `security` SET `type` = 5 WHERE id LIKE 'ASSET:%';
UPDATE `security` SET `type` = 3
WHERE length(id) <> 12
  AND id NOT LIKE '______\_%'
  AND id NOT LIKE 'ASSET:%';

ALTER TABLE `security` ADD KEY `security_type_ix` (`type`);
ALTER TABLE `security` ADD CONSTRAINT `security_type_fkey`
    FOREIGN KEY (`type`) REFERENCES `security_type` (`id`) ON UPDATE CASCADE ON DELETE RESTRICT;

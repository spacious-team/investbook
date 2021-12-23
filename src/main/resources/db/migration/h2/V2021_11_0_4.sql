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

-- previous steps in V2021_11_0_3.sql

ALTER TABLE `security` RENAME COLUMN `security_id` TO `id`;
ALTER TABLE `security_description` RENAME COLUMN `security_id` TO `security`;
ALTER TABLE `security_event_cash_flow` RENAME COLUMN `security_id` TO `security`;
ALTER TABLE `security_quote` RENAME COLUMN `security_id` TO `security`;
ALTER TABLE `transaction` RENAME COLUMN `security_id` TO `security`;

ALTER TABLE `security` ALTER COLUMN `id` SET NOT NULL;
ALTER TABLE `security_description` ALTER COLUMN `security` SET NOT NULL;
ALTER TABLE `security_event_cash_flow` ALTER COLUMN `security` SET NOT NULL;
ALTER TABLE `security_quote` ALTER COLUMN `security` SET NOT NULL;
ALTER TABLE `transaction` ALTER COLUMN `security` SET NOT NULL;

-- primary key should be NOT NULL
ALTER TABLE `security_description` ADD PRIMARY KEY (`security`);
ALTER TABLE `security_description` ADD CONSTRAINT `security_description_security_fkey`
    FOREIGN KEY (`security`) REFERENCES `security` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;

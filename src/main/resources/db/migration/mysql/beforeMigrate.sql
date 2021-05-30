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

-- Исправляет проблему запуска патча V2020_13_0_0.sql поверх schema.sql из версии 2021.5
-- Можно убрать для flyway.baseline-version >= 2021.5
-- ALTER TABLE `security_quote`
--     ADD KEY IF NOT EXISTS `security_quote_security_fkey` (`security`);
-- ALTER TABLE `security_quote`
--     DROP KEY IF EXISTS `security_quote_security_ix`;

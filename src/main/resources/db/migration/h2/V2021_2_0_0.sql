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

ALTER TABLE `event_cash_flow`
    DROP FOREIGN KEY `event_cash_flow_portfolio_fkey`;
ALTER TABLE `event_cash_flow`
    ADD CONSTRAINT `event_cash_flow_portfolio_fkey` FOREIGN KEY (`portfolio`)
        REFERENCES `portfolio` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `portfolio_property`
    DROP FOREIGN KEY `portfolio_property_portfolio_fkey`;
ALTER TABLE `portfolio_property`
    ADD CONSTRAINT `portfolio_property_portfolio_fkey` FOREIGN KEY (`portfolio`)
        REFERENCES `portfolio` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `security_event_cash_flow`
    DROP FOREIGN KEY `security_event_cash_flow_portfolio_fkey`;
ALTER TABLE `security_event_cash_flow`
    ADD CONSTRAINT `security_event_cash_flow_portfolio_fkey` FOREIGN KEY (`portfolio`)
        REFERENCES `portfolio` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `transaction`
    DROP FOREIGN KEY `transaction_portfolio_fkey`;
ALTER TABLE `transaction`
    ADD CONSTRAINT `transaction_portfolio_fkey` FOREIGN KEY (`portfolio`)
        REFERENCES `portfolio` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `security_quote`
    DROP CONSTRAINT IF EXISTS `security_quote_isin_fkey`;
ALTER TABLE `security_quote`
    ADD CONSTRAINT IF NOT EXISTS `security_quote_security_fkey` FOREIGN KEY (`security`)
        REFERENCES `security` (`id`) ON UPDATE CASCADE ON DELETE CASCADE;
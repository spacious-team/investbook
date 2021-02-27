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

-- gh-223: rename uniq index and foreign key was renamed in schema after 2020.13
ALTER TABLE `security_event_cash_flow`
    DROP CONSTRAINT IF EXISTS `security_event_cash_flow_isin_fkey`;
ALTER TABLE `security_event_cash_flow`
    ADD CONSTRAINT `security_event_cash_flow_security_fkey` FOREIGN KEY IF NOT EXISTS (`security`)
    REFERENCES `security` (`id`) ON UPDATE CASCADE;

ALTER TABLE `security_event_cash_flow`
    DROP CONSTRAINT IF EXISTS `security_event_cash_flow_timestamp_isin_type_portfolio_uniq_ix`;
ALTER TABLE `security_event_cash_flow`
    ADD CONSTRAINT `security_event_cash_flow_timestamp_security_type_portfolio_uniq` UNIQUE IF NOT EXISTS(`timestamp`,`security`,`type`,`portfolio`);



ALTER TABLE `security_quote`
    DROP CONSTRAINT IF EXISTS `security_quote_isin_timestamp_uniq_ix`;
ALTER TABLE `security_quote`
    ADD CONSTRAINT `security_quote_security_timestamp_uniq_ix` UNIQUE IF NOT EXISTS(`security`, `timestamp`);


ALTER TABLE `transaction`
    DROP CONSTRAINT IF EXISTS `transaction_isin_fkey`;
ALTER TABLE `transaction`
    ADD CONSTRAINT `transaction_security_fkey` FOREIGN KEY IF NOT EXISTS (`security`)
    REFERENCES `security` (`id`) ON UPDATE CASCADE;

ALTER TABLE `transaction`
    ADD KEY IF NOT EXISTS `transaction_security_ix` (`security`);
ALTER TABLE `transaction`
    DROP KEY IF EXISTS `transaction_ticker_ix`;
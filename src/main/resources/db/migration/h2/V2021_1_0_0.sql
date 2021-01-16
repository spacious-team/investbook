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

/*!40000 ALTER TABLE `stock_market_index` DISABLE KEYS */;
INSERT IGNORE INTO `stock_market_index` (`date`, `sp500`) VALUES
('2020-12-14', 3647.49),
('2020-12-15', 3694.62),
('2020-12-16', 3701.17),
('2020-12-17', 3722.48),
('2020-12-18', 3709.41),
('2020-12-21', 3694.92),
('2020-12-22', 3687.26),
('2020-12-23', 3690.01),
('2020-12-24', 3703.06),
('2020-12-28', 3735.36),
('2020-12-29', 3727.04),
('2020-12-30', 3732.04),
('2020-12-31', 3756.07),
('2021-01-04', 3700.65),
('2021-01-05', 3726.86),
('2021-01-06', 3748.14),
('2021-01-07', 3803.79),
('2021-01-08', 3824.68),
('2021-01-11', 3799.61),
('2021-01-12', 3801.19),
('2021-01-13', 3809.84),
('2021-01-14', 3795.54),
('2021-01-15', 3768.25);
/*!40000 ALTER TABLE `stock_market_index` ENABLE KEYS */;
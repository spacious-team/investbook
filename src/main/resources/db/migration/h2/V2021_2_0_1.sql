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
('2021-01-19', 3798.90),
('2021-01-20', 3851.85),
('2021-01-21', 3853.07),
('2021-01-22', 3841.46),
('2021-01-25', 3855.36),
('2021-01-26', 3849.62),
('2021-01-27', 3750.77),
('2021-01-28', 3787.37),
('2021-01-29', 3714.23),
('2021-02-01', 3773.86),
('2021-02-02', 3826.31),
('2021-02-03', 3830.16),
('2021-02-04', 3871.73),
('2021-02-05', 3886.83),
('2021-02-08', 3915.59),
('2021-02-09', 3911.22),
('2021-02-10', 3909.87),
('2021-02-11', 3916.37),
('2021-02-12', 3934.83),
('2021-02-16', 3932.59),
('2021-02-17', 3931.33),
('2021-02-18', 3913.96),
('2021-02-19', 3906.70),
('2021-02-22', 3881.52);
/*!40000 ALTER TABLE `stock_market_index` ENABLE KEYS */;
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

INSERT IGNORE INTO `foreign_exchange_rate` (`date`, `currency_pair`, `rate`)
    SELECT DISTINCT `timestamp`, substr(`property`, 0, 6), `value`
    FROM PORTFOLIO_PROPERTY where `property` LIKE '______\_EXCHANGE\_RATE';

DELETE FROM `portfolio_property`
    WHERE `property` LIKE '______\_EXCHANGE\_RATE';

UPDATE `portfolio_property`
    SET `property` = 'TOTAL_ASSETS_RUB', `timestamp` = `timestamp`
    WHERE `property` = 'TOTAL_ASSETS';

/*!40000 ALTER TABLE `foreign_exchange_rate` DISABLE KEYS */;
INSERT IGNORE INTO `foreign_exchange_rate` (`date`, `currency_pair`, `rate`) VALUES
('2020-01-01', 'USDRUB', 61.9057),
('2020-01-10', 'USDRUB', 61.234),
('2020-01-11', 'USDRUB', 61.2632),
('2020-01-14', 'USDRUB', 60.9474),
('2020-01-15', 'USDRUB', 61.414),
('2020-01-16', 'USDRUB', 61.4328),
('2020-01-17', 'USDRUB', 61.5694),
('2020-01-18', 'USDRUB', 61.5333),
('2020-01-21', 'USDRUB', 61.4654),
('2020-01-22', 'USDRUB', 61.8552),
('2020-01-23', 'USDRUB', 61.8343),
('2020-01-24', 'USDRUB', 61.9515),
('2020-01-25', 'USDRUB', 61.8031),
('2020-01-28', 'USDRUB', 62.338),
('2020-01-29', 'USDRUB', 62.8299),
('2020-01-30', 'USDRUB', 62.3934),
('2020-01-31', 'USDRUB', 63.0359),
('2020-02-01', 'USDRUB', 63.1385),
('2020-02-04', 'USDRUB', 63.9091),
('2020-02-05', 'USDRUB', 63.4342),
('2020-02-06', 'USDRUB', 63.1742),
('2020-02-07', 'USDRUB', 62.7977),
('2020-02-08', 'USDRUB', 63.472),
('2020-02-11', 'USDRUB', 63.7708),
('2020-02-12', 'USDRUB', 63.949),
('2020-02-13', 'USDRUB', 63.047),
('2020-02-14', 'USDRUB', 63.6016),
('2020-02-15', 'USDRUB', 63.4536),
('2020-02-18', 'USDRUB', 63.3085),
('2020-02-19', 'USDRUB', 63.7698),
('2020-02-20', 'USDRUB', 63.6873),
('2020-02-21', 'USDRUB', 63.7413),
('2020-02-22', 'USDRUB', 64.3008),
('2020-02-26', 'USDRUB', 64.9213),
('2020-02-27', 'USDRUB', 65.5177),
('2020-02-28', 'USDRUB', 65.6097),
('2020-02-29', 'USDRUB', 66.9909),
('2020-03-03', 'USDRUB', 66.3274),
('2020-03-04', 'USDRUB', 66.4437),
('2020-03-05', 'USDRUB', 66.0784),
('2020-03-06', 'USDRUB', 66.1854),
('2020-03-07', 'USDRUB', 67.5175),
('2020-03-11', 'USDRUB', 72.0208),
('2020-03-12', 'USDRUB', 71.472),
('2020-03-13', 'USDRUB', 74.0274),
('2020-03-14', 'USDRUB', 73.1882),
('2020-03-17', 'USDRUB', 74.1262),
('2020-03-18', 'USDRUB', 73.8896),
('2020-03-19', 'USDRUB', 77.2131),
('2020-03-20', 'USDRUB', 80.157),
('2020-03-21', 'USDRUB', 78.0443),
('2020-03-24', 'USDRUB', 80.8815),
('2020-03-25', 'USDRUB', 78.8493),
('2020-03-26', 'USDRUB', 77.7928),
('2020-03-27', 'USDRUB', 78.7223),
('2020-03-28', 'USDRUB', 77.7325),
('2020-04-07', 'USDRUB', 76.4074),
('2020-04-08', 'USDRUB', 75.455),
('2020-04-09', 'USDRUB', 75.7499),
('2020-04-10', 'USDRUB', 74.605),
('2020-04-11', 'USDRUB', 73.7515),
('2020-04-14', 'USDRUB', 73.5245),
('2020-04-15', 'USDRUB', 73.315),
('2020-04-16', 'USDRUB', 73.7145),
('2020-04-17', 'USDRUB', 74.7119),
('2020-04-18', 'USDRUB', 73.9441),
('2020-04-21', 'USDRUB', 74.6657),
('2020-04-22', 'USDRUB', 76.2562),
('2020-04-23', 'USDRUB', 77.0416),
('2020-04-24', 'USDRUB', 75.129),
('2020-04-25', 'USDRUB', 74.7163),
('2020-04-28', 'USDRUB', 74.496),
('2020-04-29', 'USDRUB', 74.5706),
('2020-04-30', 'USDRUB', 73.6894),
('2020-05-01', 'USDRUB', 72.7263),
('2020-05-07', 'USDRUB', 73.9719),
('2020-05-08', 'USDRUB', 74.1169),
('2020-05-09', 'USDRUB', 73.8725),
('2020-05-13', 'USDRUB', 73.4326),
('2020-05-14', 'USDRUB', 73.5819),
('2020-05-15', 'USDRUB', 73.9298),
('2020-05-16', 'USDRUB', 73.2056),
('2020-05-19', 'USDRUB', 72.9798),
('2020-05-20', 'USDRUB', 72.3918),
('2020-05-21', 'USDRUB', 72.3381),
('2020-05-22', 'USDRUB', 70.924),
('2020-05-23', 'USDRUB', 71.8804),
('2020-05-26', 'USDRUB', 71.5962),
('2020-05-27', 'USDRUB', 71.1408),
('2020-05-28', 'USDRUB', 71.0635),
('2020-05-29', 'USDRUB', 71.1012),
('2020-05-30', 'USDRUB', 70.752),
('2020-06-02', 'USDRUB', 69.7114),
('2020-06-03', 'USDRUB', 68.9831),
('2020-06-04', 'USDRUB', 68.3413),
('2020-06-05', 'USDRUB', 69.0151),
('2020-06-06', 'USDRUB', 68.6319),
('2020-06-09', 'USDRUB', 68.3123),
('2020-06-10', 'USDRUB', 68.6745),
('2020-06-11', 'USDRUB', 68.6183),
('2020-06-12', 'USDRUB', 69.1219),
('2020-06-16', 'USDRUB', 70.395),
('2020-06-17', 'USDRUB', 69.7524),
('2020-06-18', 'USDRUB', 69.4822),
('2020-06-19', 'USDRUB', 69.618),
('2020-06-20', 'USDRUB', 69.5725),
('2020-06-23', 'USDRUB', 69.4835),
('2020-06-24', 'USDRUB', 68.8376),
('2020-06-25', 'USDRUB', 68.8376),
('2020-06-26', 'USDRUB', 69.466),
('2020-06-27', 'USDRUB', 69.1284),
('2020-06-30', 'USDRUB', 69.9513),
('2020-07-01', 'USDRUB', 70.4413),
('2020-07-02', 'USDRUB', 70.4413),
('2020-07-03', 'USDRUB', 70.5198),
('2020-07-04', 'USDRUB', 70.4999),
('2020-07-07', 'USDRUB', 71.3409),
('2020-07-08', 'USDRUB', 72.1719),
('2020-07-09', 'USDRUB', 71.2379),
('2020-07-10', 'USDRUB', 70.88),
('2020-07-11', 'USDRUB', 71.2298),
('2020-07-14', 'USDRUB', 70.7479),
('2020-07-15', 'USDRUB', 71.1275),
('2020-07-16', 'USDRUB', 70.7998),
('2020-07-17', 'USDRUB', 71.231),
('2020-07-18', 'USDRUB', 71.7139),
('2020-07-21', 'USDRUB', 71.9628),
('2020-07-22', 'USDRUB', 70.9668),
('2020-07-23', 'USDRUB', 70.7881),
('2020-07-24', 'USDRUB', 70.963),
('2020-07-25', 'USDRUB', 71.5974),
('2020-07-28', 'USDRUB', 71.585),
('2020-07-29', 'USDRUB', 71.9196),
('2020-07-30', 'USDRUB', 72.2348),
('2020-07-31', 'USDRUB', 73.3633),
('2020-08-01', 'USDRUB', 73.4261),
('2020-08-04', 'USDRUB', 74.1586),
('2020-08-05', 'USDRUB', 73.3806),
('2020-08-06', 'USDRUB', 73.2806),
('2020-08-07', 'USDRUB', 73.0397),
('2020-08-08', 'USDRUB', 73.6376),
('2020-08-11', 'USDRUB', 73.775),
('2020-08-12', 'USDRUB', 73.1522),
('2020-08-13', 'USDRUB', 73.2351),
('2020-08-14', 'USDRUB', 73.6067),
('2020-08-15', 'USDRUB', 73.2157),
('2020-08-18', 'USDRUB', 72.9676),
('2020-08-19', 'USDRUB', 73.4321),
('2020-08-20', 'USDRUB', 73.2392),
('2020-08-21', 'USDRUB', 73.7711),
('2020-08-22', 'USDRUB', 74.0999),
('2020-08-25', 'USDRUB', 74.4184),
('2020-08-26', 'USDRUB', 74.5126),
('2020-08-27', 'USDRUB', 75.5379),
('2020-08-28', 'USDRUB', 75.2354),
('2020-08-29', 'USDRUB', 74.6382),
('2020-09-01', 'USDRUB', 73.8039),
('2020-09-02', 'USDRUB', 73.5849),
('2020-09-03', 'USDRUB', 73.8588),
('2020-09-04', 'USDRUB', 75.468),
('2020-09-05', 'USDRUB', 75.1823),
('2020-09-08', 'USDRUB', 75.591),
('2020-09-09', 'USDRUB', 75.9645),
('2020-09-10', 'USDRUB', 76.0713),
('2020-09-11', 'USDRUB', 75.5274),
('2020-09-12', 'USDRUB', 74.8896),
('2020-09-15', 'USDRUB', 74.7148),
('2020-09-16', 'USDRUB', 75.1884),
('2020-09-17', 'USDRUB', 74.9278),
('2020-09-18', 'USDRUB', 75.1941),
('2020-09-19', 'USDRUB', 75.0319),
('2020-09-22', 'USDRUB', 76.0381),
('2020-09-23', 'USDRUB', 76.2711),
('2020-09-24', 'USDRUB', 76.3545),
('2020-09-25', 'USDRUB', 77.178),
('2020-09-26', 'USDRUB', 76.8195),
('2020-09-29', 'USDRUB', 78.6713),
('2020-09-30', 'USDRUB', 79.6845),
('2020-10-01', 'USDRUB', 78.7847),
('2020-10-02', 'USDRUB', 77.2774),
('2020-10-03', 'USDRUB', 78.0915),
('2020-10-06', 'USDRUB', 78.1281),
('2020-10-07', 'USDRUB', 78.5119),
('2020-10-08', 'USDRUB', 78.0921),
('2020-10-09', 'USDRUB', 77.9157),
('2020-10-10', 'USDRUB', 77.0284),
('2020-10-13', 'USDRUB', 77.0239),
('2020-10-14', 'USDRUB', 77.2855),
('2020-10-15', 'USDRUB', 77.2759),
('2020-10-16', 'USDRUB', 77.9461),
('2020-10-17', 'USDRUB', 77.9644),
('2020-10-20', 'USDRUB', 77.9241),
('2020-10-21', 'USDRUB', 77.778),
('2020-10-22', 'USDRUB', 77.0322),
('2020-10-23', 'USDRUB', 77.0809),
('2020-10-24', 'USDRUB', 76.4667),
('2020-10-27', 'USDRUB', 76.4443),
('2020-10-28', 'USDRUB', 76.4556),
('2020-10-29', 'USDRUB', 77.552),
('2020-10-30', 'USDRUB', 78.8699),
('2020-10-31', 'USDRUB', 79.3323),
('2020-11-03', 'USDRUB', 80.5749),
('2020-11-04', 'USDRUB', 80.0006),
('2020-11-06', 'USDRUB', 78.4559),
('2020-11-07', 'USDRUB', 77.1875),
('2020-11-10', 'USDRUB', 76.9515),
('2020-11-11', 'USDRUB', 76.3978),
('2020-11-12', 'USDRUB', 76.2075),
('2020-11-13', 'USDRUB', 77.1148),
('2020-11-14', 'USDRUB', 77.3262),
('2020-11-17', 'USDRUB', 76.9197),
('2020-11-18', 'USDRUB', 76.253),
('2020-11-19', 'USDRUB', 75.9268),
('2020-11-20', 'USDRUB', 76.2627),
('2020-11-21', 'USDRUB', 76.012),
('2020-11-24', 'USDRUB', 75.76),
('2020-11-25', 'USDRUB', 75.8146),
('2020-11-26', 'USDRUB', 75.4727),
('2020-11-27', 'USDRUB', 75.4518),
('2020-11-28', 'USDRUB', 75.8599),
('2020-12-01', 'USDRUB', 76.1999),
('2020-12-02', 'USDRUB', 76.3203),
('2020-12-03', 'USDRUB', 75.6151),
('2020-12-04', 'USDRUB', 75.1996),
('2020-12-05', 'USDRUB', 74.2529),
('2020-12-08', 'USDRUB', 74.2506),
('2020-12-09', 'USDRUB', 73.6618),
('2020-12-10', 'USDRUB', 73.3057),
('2020-12-11', 'USDRUB', 73.7124),
('2020-12-12', 'USDRUB', 73.1195),
('2020-12-15', 'USDRUB', 72.9272),
('2020-12-16', 'USDRUB', 73.4453),
('2020-12-17', 'USDRUB', 73.4201),
('2020-12-18', 'USDRUB', 72.9781),
('2020-12-19', 'USDRUB', 73.3155),
('2020-12-22', 'USDRUB', 74.6721),
('2020-12-23', 'USDRUB', 75.3498),
('2020-12-24', 'USDRUB', 75.4571),
('2020-12-25', 'USDRUB', 74.8392),
('2020-12-26', 'USDRUB', 73.6921),
('2020-12-29', 'USDRUB', 73.7175),
('2020-12-30', 'USDRUB', 73.6567),
('2020-12-31', 'USDRUB', 73.8757),
('2021-01-01', 'USDRUB', 73.8757),
('2021-01-12', 'USDRUB', 74.5157),
('2021-01-13', 'USDRUB', 74.2663),
('2021-01-14', 'USDRUB', 73.5264),
('2021-01-15', 'USDRUB', 73.7961),
('2021-01-16', 'USDRUB', 73.5453);
/*!40000 ALTER TABLE `foreign_exchange_rate` ENABLE KEYS */;
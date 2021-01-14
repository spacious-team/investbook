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

-- Дамп данных таблицы portfolio.cash_flow_type: ~13 rows (приблизительно)
/*!40000 ALTER TABLE `cash_flow_type` DISABLE KEYS */;
INSERT IGNORE INTO `cash_flow_type` (`id`, `name`) VALUES
(0, 'Пополнение и снятие'),
(1, 'Чистая стоимость сделки (без НКД)'),
(2, 'НКД на день сделки'),
(3, 'Комиссия'),
(4, 'Амортизация облигации'),
(5, 'Погашение облигации'),
(6, 'Купонный доход (до удержания налога)'),
(7, 'Дивиденды (до удержания налога)'),
(8, 'Вариационная маржа'),
(9, 'Гарантийное обеспечение'),
(10, 'Налог уплаченный (с купона, с дивидендов)'),
(11, 'Прогнозируемый налог'),
(12, 'Стоимость сделки с деривативом, валюта (RUB, USD или др.)'),
(13, 'Стоимость сделки с деривативом, пункты');
/*!40000 ALTER TABLE `cash_flow_type` ENABLE KEYS */;

#
# InvestBook
# Copyright (C) 2020  Vitalii Ananev (an-vitek@ya.ru)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

# Задать путь к распакованному архиву с Java
#set JAVA_HOME=C:\Program Files\Java\jdk-15

# Запуск приложения
chcp 65001
set PATH=%JAVA_HOME%\bin;%PATH%
for %%A in (*.jar) do set jarfile=%%A
java -jar %jarfile%
pause
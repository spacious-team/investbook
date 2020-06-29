# Задать путь к распакованному архиву с Java
set JAVA_HOME=C:\Program Files\Java\jdk-14

# Запуск приложения
chcp 65001
set PATH=%JAVA_HOME%\bin;%PATH%
for %%A in (*.jar) do set jarfile=%%A
java -jar %jarfile%
pause
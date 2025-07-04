### Инструкция для начинающих
Если вы начинающий разработчик воспользуйтесь этой [инструкцией](https://github.com/spacious-team/investbook/files/5398264/github.docx).
Если вы намерены изменить документацию, то эта инструкция самодостаточна, поэтому остальные пункты на этой странице
вам читать не нужно.

### Клонирование кода и импорт в среду разработки
Клонируйте репозиторий
```
git clone https://github.com/spacious-team/investbook.git
```
Может использоваться любая среда разработки, для простых изменений можно ограничиться текстовым редактором.
Если вы используете [IntelliJ IDEA](https://www.jetbrains.com/ru-ru/idea/download), файлы настроек среды разработки
уже сохранены в репозиторий, просто импортируйте код как maven проект.

### Установка java
Для разработки требуется версия java, которая указана в файле [pom.xml](../pom.xml) в теге `<java.version>`.
Скачайте java под свою ОС, например с сайта [OpenJDK](https://openjdk.org/install/), и установите переменные
окружения `JAVA_HOME` и `PATH`, например для Windows 10 по этой [инструкции](https://csharpcoderr.com/5351/).

### Установка Wix
Эта глава относится только к тем, кто работает на Windows.
Для сборки msi пакета, устанавливающего Investbook на Windows, требуется Wix.
Wix в свою очередь требует установки [.NET](https://dotnet.microsoft.com/en-us/download/dotnet).

Вы можете установить .NET без прав администратора в директорию `%LOCALAPPDATA%\Programs\dotnet`.
Для этого нужно скачать не msi установщик, а архив "binaries", который требуется распаковать в указанную папку.
После этого нужно отредактировать переменные окружения (win+R -> `rundll32 sysdm.cpl,EditEnvironmentVariables`):
```shell
DOTNET_ROOT=%LOCALAPPDATA%\Programs\dotnet
PATH=<предыдущие значения>;%DOTNET_ROOT%
```

Далее в командной нужно проверить, установлен ли Wix
```shell
dotnet tool list --global
```
Если Wix не установлен, то его требуется установить по [инструкции](https://docs.firegiant.com/wix/using-wix/)
```shell
dotnet tool install --global wix
wix --version
```
Также требуется установить расширения Wix, без которых сборка завершается с
[ошибкой](https://github.com/petr-panteleyev/jpackage-gradle-plugin/issues/38)
```shell
wix extension add -g WixToolset.Util.wixext/6.0.1
wix extension add -g WixToolset.Ui.wixext/6.0.1
```
где 6.0.1 - это версия Wix, которая указана в выводе команды
```shell
dotnet tool list --global
```

### Компиляция
Компиляция запускается командой:
```shell
./mvnw clean compile
```
Она очищает сгенерированные ранее классы (например JAXB2), которые возможно устарели,
и генерирует файл `META_INF/build-info.properties`, который используется приложением в своей работе.

### Запуск
Перед запуском приложения средствами IntelliJ IDEA обязательна [компиляция](#компиляция), после которой можно
запустить приложение по кнопке в верхней панели.

Если у вас другая среда разработки или вы работаете из консоли, то приложение можно запустить без предварительной
компиляции командой:
```shell
./mvnw spring-boot:run
```

### Сборка релиза
Если вы работаете на Windows, то перед сборкой релиза необходимо установить [Wix](#установка-wix).

Для сборки релиза запустите:
```shell
./mvnw package
```
В зависимости от ОС в папке `target/installer/output/` соберется:
- msi инсталлятор на Windows;
- deb и rpm пакет на Unix;
- pkg пакет на Mac.
Portable версия в zip архиве собирается на любой ОС в папке `target/`.

### Обновление maven wrapper
Если требуется обновить maven wrapper, выполнить
```shell
mvn wrapper:wrapper -Dtype=only-script
```

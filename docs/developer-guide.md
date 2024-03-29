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
Для сборки пакета для установки Investbook требуется Wix 3.
Скачать Wix 3 можно по ссылке с официального сайта [Wix](https://wixtoolset.org/docs/wix3/)
(пакет для установки расположен на [GitHub](https://github.com/wixtoolset/wix3/releases)).
Wix в свою очередь потребует установки [.NET](https://dotnet.microsoft.com/en-us/download/dotnet).

### Компиляция
Компиляция запускается командой:
```
mvn clean compile
```
Она очищает сгенерированные ранее классы (типа JAXB), которые возможно устарели,
и генерирует файл `META_INF/build-info.properties`, который используется приложением в своей работе.

### Запуск
Перед запуском приложения средствами IntelliJ IDEA обязательна [компиляция](#компиляция), после которой можно
запустить приложение по кнопке в верхней панели.

Если у вас другая среда разработки или вы работаете из консоли, то приложение можно запустить без предварительной
компиляции командой:
```
mvn spring-boot:run
```

### Сборка релиза
Релиз состоит из двух файлов: zip-архива и msi-установщика. Msi-установщик собирается только на Windows.
Поэтому если вы работаете под Windows, необходимо установить `Wix` со страницы [проекта](https://wixtoolset.org/releases/)
(_WiX Toolset Visual Studio Extension_ устанавливать не нужно).

Для сборки релиза запустите
```
mvn package
```
Zip-архив может быть [установлен](install-on-linux.md) на Linux и Mac. На Windows рекомендуется
[установка](install-on-windows.md) через msi-инсталлятор.

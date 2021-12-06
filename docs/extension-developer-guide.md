### Разработка расширения для поддержки нового Брокера
Investbook поддерживает конечный список поддерживаемых брокеров из "коробки", который может быть расширен за счет
расширений. Расширения могут распространяться вами бесплатно или на платной основе под своим брендом.
Разработка расширений должна вестись в вашем собственном репозитории с использованием лицензии совместимой с
GNU Affero GPLv3.

#### Как написать код
Создайте новый, например maven, проект и добавьте зависимости
```xml
<packaging>jar</packaging>

<repositories>
    <repository>
        <id>central</id>
        <name>Central Repository</name>
        <url>https://repo.maven.apache.org/maven2</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.spacious-team</groupId>
        <artifactId>broker-report-parser-api</artifactId>
        <version>RELEASE</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.github.spacious-team</groupId>
        <artifactId>table-wrapper-excel-impl</artifactId>
        <!-- ... или table-wrapper-xml-impl, если брокер предоставляет отчеты в xml файле -->
        <version>RELEASE</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
        <version>RELEASE</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>RELEASE</version>
        <scope>provided</scope>
        <optional>true</optional>
    </dependency>
</dependencies>
```
Зависимости добавляются со scope provided, т.к. необходимы только для компиляции. В runtime указанные зависимости
предоставит приложение Investbook.

Далее напишите классы согласно документации
[Broker Report Parser API](https://github.com/spacious-team/broker-report-parser-api#%D0%B4%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82%D0%B0%D1%86%D0%B8%D1%8F-%D0%BF%D0%BE-%D1%80%D0%B0%D0%B7%D1%80%D0%B0%D0%B1%D0%BE%D1%82%D0%BA%D0%B5).
По окончании разработки укажите на фабриках, реализующих интерфейсы `BrokerReportFactory` и `ReportTablesFactory`
аннотации `@Component`, чтобы приложение Investbook могло создать объекты фабрик через механизм IoC:

```java
@org.springframework.stereotype.Component
public class MyBrokerReportFactory implements BrokerReportFactory {
    // ...
}

@org.springframework.stereotype.Component
public class MyReportTablesFactory implements ReportTablesFactory {
    // ...
}
```
Расширение для брокера готово. Соберите jar-архив
```shell script
mvn clean install
```

#### Тестирование расширения
Выгрузите исходный код Investbook из [репозитория](https://github.com/spacious-team/investbook) и в pom.xml
в секции "dependencies" подключите ваше расширение
```
<dependencies>
    ...
    <dependency>
        <groupId>your-group-id</groupId>
        <artifactId>your-artifact</artifactId>
        <version>RELEASE</version>
    </dependency>
</dependencies>
```
Запустите Investbook и [загрузите](/docs/install-on-windows.md) отчеты нового брокера.

#### Распространение расширения
Вы можете передать jar-архив с вашим расширением другим пользователям приложения Investbook платно или бесплатно.
Проинструктируйте пользователей поместить jar файл расширения в директорию
- `<директория-установки>/app/extensions/` (если приложение установлено через msi инсталлятор) или
- `<директория-установки>/extensions/` (если приложение установлено из zip архива)

и перезапустить приложение. После этого пользователи увидят наименование вашего брокера, возвращаемое
`ReportFactory.getBrokerName()`, в списке поддерживаемых брокеров.
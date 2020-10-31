### Разработка расширения для поддержки нового Брокера

#### Как написать код
Investbook поддерживает конечное количество брокеров из "коробки". Расширения позволяют написать парсер отчетов
для отсутствующих Брокеров и распространять среди пользователей приложения бесплатно или на платной основе под своим
брендом. Разработка должна вестить в вашем собственном репозитории c использованием лицензии совместимой с
GNU Affero GPLv3.

Создайте, например, новый maven проект и добавьте зависимости
```xml
<packaging>jar</packaging>

<repositories>
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

Главным объектом расширения является объект, реализующий интерфейс `BrokerReport`. Этот класс является оберткой
над файлом-отчетом брокера, из которого в дальнейшем будет получена информация. Создайте этот класс, например
`MyBrokerReport`, и предоставьте его приложению через фабрику
```
@Component
@Slf4j
public class MyBrokerReportFactory extends AbstractBrokerReportFactory {
    
    @Getter
    private final String brokerName = "MyBroker";
    private final Pattern expectedFileNamePattern = Pattern.compile("^My_broker_[0-9()\\-_]+\\.xml$");

    @Override
    public BrokerReport create(String excelFileName, InputStream is) {
        BrokerReport brokerReport = create(expectedFileNamePattern, excelFileName, is, MyBrokerReport::new);
        if (brokerReport != null) {
            log.info("Обнаружен отчет '{}' валютного рынка Промсвязьбанк брокера", excelFileName);
        }
        return brokerReport;
    }
}
```

Далее реализуйте интерфейс `ReportTables`, который предоставит информацию из отчета брокера в виде объектов `ReportTable`.
Пример реализации класса `ReportTables` можно посмотреть по
[ссылке](https://github.com/spacious-team/investbook/blob/develop/src/main/java/ru/investbook/parser/psb/foreignmarket/PsbForeignMarketReportTables.java)
```java
public class MyReportTables implements ReportTables {
    
    @Override
    public ReportTable<Security> getSecuritiesTable() {
        return new EmptyReportTable<>(report);
    }
    // другие методы ...
```
Пример реализации `ReportTable` также в свободном
[доступе](https://github.com/spacious-team/investbook/blob/develop/src/main/java/ru/investbook/parser/psb/SecuritiesTable.java).

Обратите внимание, ответ брокера может не содержать всей информации, например если брокер не предоставляет информации
о котировках, нужно возвращать пустую таблицу `EmptyReportTable`. На первом этапе вы также можете парсить из отчета
брокера только часть информационных таблиц, для оставшихся просто верните `EmptyReportTable`.

Когда клас `ReportTables` реализован нужно создать фабрику, возвращающую объект этого класса
```java
@org.springframework.stereotype.Component
public class MyReportTablesFactory implements ReportTablesFactory {
    @Override
    public boolean canCreate(BrokerReport brokerReport) {
        return (brokerReport instanceof ByBrokerReport);
    }

    @Override
    public ReportTables create(BrokerReport brokerReport) {
        return new MyReportTables(brokerReport);
    }
}
```
Расширение готово загружать данные в Investbook. Соберите jar-архив вашего расширения
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

#### Распространиение расширения
Вы можете передать jar-архив с вашим расширением другим пользователям приложения Investbook платно или бесплатно.
Пользователи, получив расширение, сохранят его в директории
- `<директория-установки>/app/extensions/` (если приложение установлено через msi инсталлятор) или
- `<директория-установки>/extensions/` (если приложение установлено из zip архива)
и, перезапустив приложение, смогут использовать расширение.
![java-version](https://img.shields.io/badge/Java-15-brightgreen?style=flat-square)
![spring-boot-version](https://img.shields.io/badge/SpringBoot-2.3.4-brightgreen?style=flat-square)
![jpackage-version](https://img.shields.io/badge/Jpackage-latest-brightgreen?style=flat-square)

<img src="https://user-images.githubusercontent.com/11336712/85948992-b1d6de00-b95c-11ea-8edc-4d5e7dfc8210.png" width="100%"/>

#### Оглавление
- [Назначение](#назначение)
- [Установка](#установка)
- [Работа с приложением](#работа-с-приложением)
- [Обновление приложения](#обновление-приложения)
- [Брокеры](#брокеры)  
- [Документация](#документация)
- [Лицензия](#лицензия)
- [Почему код приложения открыт](#почему-код-приложения-открыт)
- [Как помочь](#как-помочь)

### Назначение
Если вы ведете учет сделок в excel или слышали, что его надо вести
([рекомендация 1](https://zen.yandex.ru/media/openjournal/kak-vesti-uchet-sdelok-v-excel-5d52616ea98a2a00ad258284),
[2](https://vse-dengy.ru/pro-investitsii/dohodnost-investitsiy-xirr.html),
[3](https://www.banki.ru/forum/?PAGE_NAME=read&FID=21&TID=325769)), то приложение поможет вам это делать.

Учет сделок в excel таблице, в отличие от отчетов брокера, показывает историю портфеля: усредненную цену покупки,
доход по сделке продажи, историю выплаченных дивидендов и купонов, удержания налогов как с выплат, так и c выводимых
со счета средств, ввод и вывод со счета денежных средств за все время, итоговую доходность инвестиций и спекуляций
с момента открытия счета, будущие удержания налогов, - это не полный перечень информации, которую рассчитывает приложение.

Все что нужно - это подгружать свежие отчеты брокера. По каждому счету и валюте будет доступна следующая информация:
- текущие позиции, усредненные цены покупок и доходности ценных бумаг в [портфеле](src/main/asciidoc/portfolio-status.adoc)
  (с учетом хеджирующих позиций на срочном рынке и усредненной цены покупки валюты);  
  ![portfolio](https://user-images.githubusercontent.com/11336712/93006504-c8590280-f565-11ea-947c-dc8fa5712844.png)
- доля ценной бумаги в [портфеле](src/main/asciidoc/portfolio-status.adoc);  
  ![current-proportion](https://user-images.githubusercontent.com/11336712/88717010-8cd6b600-d128-11ea-901f-2b3fcee96f07.png)  
- детализация дивидендных, купонных и амортизационных [выплат](src/main/asciidoc/portfolio-payment.adoc);  
  ![portfolio-payment](https://user-images.githubusercontent.com/11336712/88460806-93a2c600-cea7-11ea-8ac9-95406fd6cec8.png)
- детализация дивидендных, купонных и амортизационных [выплат](src/main/asciidoc/foreign-portfolio-payment.adoc),
  начисленные по акциям и облигациям со связанного счета ИИС;  
  ![foreign-portfolio-payment](https://user-images.githubusercontent.com/11336712/87988115-7907d000-cae8-11ea-9ec7-d56a120aac89.png)
- доходность сделок на [фондовом](src/main/asciidoc/stock-market-profit.adoc) рынке;  
  ![stock-market](https://user-images.githubusercontent.com/11336712/78156498-8de02b00-7447-11ea-833c-cfc755bd7558.png)
- доходность сделок на [срочном](src/main/asciidoc/derivatives-market-profit.adoc) рынке;  
  ![derivatives-market](https://user-images.githubusercontent.com/11336712/78156504-8f115800-7447-11ea-87e5-3cd4c34aab47.png)
- доходность сделок на [валютном](src/main/asciidoc/foreign-market-profit.adoc) рынке;  
  ![foreighn-market](https://user-images.githubusercontent.com/11336712/84881751-fa59e600-b096-11ea-8b83-19d1c1229d73.png)
- [ввод и вывод](src/main/asciidoc/securities-deposit-and-withdrawal.adoc) ценных бумаг с/на другие счета;  
  <img src="https://user-images.githubusercontent.com/11336712/87883425-f3185600-ca0f-11ea-9677-4689aa6a4ee5.png" width="40%"/>
- [доходность](src/main/asciidoc/cash-flow.adoc) портфеля, пополнения, списания, переводы с/на другие счета;  
  ![cash-in](https://user-images.githubusercontent.com/11336712/90821058-fb2f2280-e33a-11ea-858d-8941a1eebd30.png)
- [налоговая](src/main/asciidoc/tax.adoc) нагрузка;  
  ![tax](https://user-images.githubusercontent.com/11336712/96353102-b83ac280-10d1-11eb-9024-b0de4f4b153e.png)
- [комиссия](src/main/asciidoc/commission.adoc) брокера.  
  <img src="https://user-images.githubusercontent.com/11336712/92284436-a1b61e80-ef0a-11ea-9eed-9a948089bcff.png" width="65%"/>

### Установка
Скачать со страницы [проекта](https://github.com/spacious-team/investbook/releases/latest) установщик `.msi`
и запустить его.

Можете обратиться к более подробной инструкции по установке и работе с приложением для операционных систем
[windows](docs/install-on-windows.md) и [linux](docs/install-on-linux.md).

### Работа с приложением
Запустите приложение через ярлык на рабочем столе Windows, в браузере перейдите по адресу http://localhost
и загрузите отчеты брокера.

Для удобства приложение допускает:
1. Многократную загрузку одного и того же отчета (полезно, если вы не помните, загрузили конкретный отчет или нет),
   дублирования данных не произойдет.
1. Загрузку отчетов за любой временной интервал (день, месяц, год или др). Причем, допустимо, что отчеты разных временных 
   периодов будут перекрываться.
1. Допустимо загружать отчеты по нескольким брокерским/инвестиционным счетам, в том числе от разных брокерских домов.

После загрузки отчета становится доступным аналитическая выгрузка в формате excel файла. 

### Обновление приложения
Процесс обновления на Windows не отличается от процесса первоначальной установки.
Воспользуйтесь инструкцией для операционных систем [windows](docs/install-on-windows.md) и [linux](docs/install-on-linux.md).

### Брокеры
Приложение анализирует отчеты брокеров Промсвязьбанк, Уралсиб и ВТБ. В ближайшее время будет поддерживать Финам и Тинькофф.
Если ваш счет открыт у другого брокера, напишите [нам](https://t.me/investbook_support).

### Документация
Дополнительная информация может быть найдена в [документации](docs/documentation.md).

### Лицензия
Приложение является бесплатным (разрешается использовать, распространять, копировать и вносить изменения).
Текст лицензии доступен на [английском](https://www.gnu.org/licenses/agpl-3.0.html) и
[русском](http://antirao.ru/gpltrans/agplru.pdf) языках, а также доступно [пояснение](https://www.gnu.org/licenses/quick-guide-gplv3.html)
и ответы на [вопросы](https://www.gnu.org/licenses/gpl-faq.ru.html) на русском языке.

Лицензия подразумевает, что приложение передано обществу. Версия приложения на этом сайте всегда будет распространяться
бесплатно. Но лицензия также дает возможность любому разработчику улучшать собственную копию приложения в том числе
с целью ее [продажи](https://www.gnu.org/licenses/gpl-faq.ru.html#DoesTheGPLAllowMoney) (с оговоркой, что доработанный
исходный код будет открыт в сети интернет).

### Почему код приложения открыт
Идея открытого исходного кода (open source) заключается в свободе разработки и использования программного обеспечения.
Многие известные бренды используют open source, например [Instagram](https://github.com/Instagram),
[Android](https://ru.wikipedia.org/wiki/Android#%D0%98%D1%81%D1%85%D0%BE%D0%B4%D0%BD%D1%8B%D0%B9_%D0%BA%D0%BE%D0%B4),
[Telegram](https://ru.wikipedia.org/wiki/Telegram), [Google Chrome](https://ru.wikipedia.org/wiki/Google_Chrome)
и [Mozilla Firefox](https://developer.mozilla.org/en-US/docs/Mozilla/Developer_guide/Source_Code/Downloading_Source_Archives),
сайты с защищенным соединением [https](https://ru.wikipedia.org/wiki/OpenSSL), такие как https://vk.com и др.
Для некоторых сфер решения с открытым исходным кодом подходят лучше других, например в сферах финансов и шифрования данных,
т.к. этим решениям можно доверять вследствие того, что вы или любой другой желающий может посмотреть код и убедиться
в безопасности программы.

### Как помочь
Помочь можно, расширяя или корректируя [документацию](https://github.com/spacious-team/investbook/files/5398264/github.docx),
[сообщая](https://github.com/spacious-team/investbook/issues/new/choose) о проблемах в работе приложения,
[предлагая](https://github.com/spacious-team/investbook/issues/new/choose) новую функциональность или дорабатывая код.

В ближайшей перспективе появится функциональность расширений, которая позволит сторонним разработчикам расширить
список поддерживаемых "из коробки" [брокеров](#брокеры). Расширения могут быть подключены по желанию пользователей
к приложению. Сторонние разработчики могут распространять расширения бесплатно или платно. А значит разработчики,
преследуя даже разные цели - [финансовые](https://youtu.be/q4O6PX0ZuFU) или, как команда "Spacious Team",
гуманистические, - работают сообща. Если вы решили улучшать приложение в этом репозитории, ознакомьтесь, пожалуйста,
со следующей [информацией](docs/CONTRIBUTING.md).

<img src="https://user-images.githubusercontent.com/11336712/85948991-b13e4780-b95c-11ea-9df6-a28be74c489d.png" width="100%"/>
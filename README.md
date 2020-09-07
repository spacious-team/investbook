<img src="https://user-images.githubusercontent.com/11336712/85948992-b1d6de00-b95c-11ea-8edc-4d5e7dfc8210.png" width="100%"/>

#### Оглавление
- [Назначение](#назначение)
- [Установка](#установка)
- [Работа с приложением](#работа-с-приложением)
- [Обновление приложения](#обновление-приложения)
- [Документация](#документация)
- [Лицензия](#лицензия)

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
  ![portfolio](https://user-images.githubusercontent.com/11336712/92413815-7c1f5400-f15a-11ea-8ec1-bfdf6ff620c1.png)
- доля ценной бумаги (по балансовой стоимости) в портфеле;  
  ![proportion](https://user-images.githubusercontent.com/11336712/88717010-8cd6b600-d128-11ea-901f-2b3fcee96f07.png)  
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
- [налоговая](src/main/asciidoc/tax.adoc) нагрузка.  
  <img src="https://user-images.githubusercontent.com/11336712/90821578-ccfe1280-e33b-11ea-9e53-5362968d1dcf.png" width="55%"/>
- [комиссия](src/main/asciidoc/commission.adoc) брокера  
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

### Документация
Дополнительная информация может быть найдена в [документации](docs/documentation.md).

### Лицензия
Приложение является бесплатным (разрешается использовать, распространять, копировать и вносить изменения).
Текст лицензии доступен на [английском](https://www.gnu.org/licenses/gpl-3.0.html) и
[русском](http://antirao.ru/gpltrans/gplru.pdf) языках, а также доступно [пояснение](https://www.gnu.org/licenses/quick-guide-gplv3.html)
на русском языке.

<img src="https://user-images.githubusercontent.com/11336712/85948991-b13e4780-b95c-11ea-9df6-a28be74c489d.png" width="100%"/>
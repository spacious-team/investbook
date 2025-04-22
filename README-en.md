[<img src="https://github.com/spacious-team/investbook/assets/11336712/7b16c124-5230-403e-8df9-7652132e76dd" align="right"/>](README-en.md)
[<img src="https://github.com/spacious-team/investbook/assets/11336712/14847ff5-827e-4d0f-a4e9-882cb0d1397c" align="right"/>](README.md)<br/>

[![java-version](https://img.shields.io/badge/java-23-brightgreen?style=flat-square)](https://openjdk.org/)
[![spring-boot-version](https://img.shields.io/badge/spring--boot-3.4.4-brightgreen?style=flat-square)](https://github.com/spring-projects/spring-boot/releases)
[![hits-of-code](https://img.shields.io/badge/dynamic/json?style=flat-square&color=lightblue&label=hits-of-code&url=https://hitsofcode.com/github/spacious-team/investbook/json?branch=develop&query=$.count)](https://hitsofcode.com/github/spacious-team/investbook/view?branch=develop)
[![github-closed-pull-requests](https://img.shields.io/github/issues-pr-closed/spacious-team/investbook?style=flat-square&color=brightgreen)](https://github.com/spacious-team/investbook/pulls?q=is%3Apr+is%3Aclosed)
[![github-workflow-status](https://img.shields.io/github/actions/workflow/status/spacious-team/investbook/publish-docker.yml?style=flat-square&branch=master)](https://github.com/spacious-team/investbook/actions/workflows/publish-docker.yml)
[![github-all-releases](https://img.shields.io/github/downloads/spacious-team/investbook/total?style=flat-square&logo=github&color=lightblue)](https://github.com/spacious-team/investbook/releases/latest)
[![docker-pulls](https://img.shields.io/docker/pulls/spaciousteam/investbook?style=flat-square&logo=docker&color=lightblue&logoColor=white)](https://hub.docker.com/r/spaciousteam/investbook)
[![telegram-channel](https://img.shields.io/endpoint?style=flat-square&color=2ca5e0&label=news&url=https%3A%2F%2Ftg.sumanjay.workers.dev%2Finvestbook_official)](https://t.me/investbook_official)
[![telegram-group](https://img.shields.io/endpoint?style=flat-square&color=2ca5e0&label=chat&url=https%3A%2F%2Ftg.sumanjay.workers.dev%2Finvestbook_support)](https://t.me/investbook_support)
[![telegram-support](https://img.shields.io/badge/support-online-2ca5e0?style=flat-square&logo=telegram)](https://t.me/investbook_support_bot)

Find out the real annual percentage return on investment and compare it to a bank deposit, find out the average 
cost of buying stocks, bonds, derivatives, automate, analyze your portfolio.

<img src="https://user-images.githubusercontent.com/11336712/85948992-b1d6de00-b95c-11ea-8edc-4d5e7dfc8210.png" width="100%"/>

#### Contents
- [Purpose](#purpose)
- [Difference from similar products](#difference-from-similar-products)
- [Brokers](#brokers)
- [Install](#install)
- [Working with the application](#working-with-the-application)
- [Application update](#application-update)
- [Documentation](#documentation)
- [License](#license)
- [Why is the application code open source](#why-is-the-application-code-open-source)
- [How to help](#how-to-help)
- [Contacts](#contacts)

### Purpose
If you keep records of transactions in Excel or have heard that it should be kept
([recommendation 1](https://zen.yandex.ru/media/openjournal/kak-vesti-uchet-sdelok-v-excel-5d52616ea98a2a00ad258284),
[2](https://vse-dengy.ru/pro-investitsii/dohodnost-investitsiy-xirr.html),
[3](https://www.banki.ru/forum/?PAGE_NAME=read&FID=21&TID=325769)), then this free application will help you do it.

Accounting for transactions in an Excel table, in contrast to broker reports, shows the history of the portfolio: 
the average purchase price, financial results of transactions, history of dividends and coupons, withholding taxes on 
payments and withdrawals, history of cash flows, the final return on investments and speculation since the opening of 
the account, future tax withholding and tax obligations are not a complete list of information that the application will
calculate for you.

Some brokers provide a personal account and charts, but may not disclose all the information. If you have
several accounts with different brokers, information will be presented in different places, volumes and formats. Application
objectively, displays data in a single format for all brokers.

![main-page](https://user-images.githubusercontent.com/11336712/128609729-08b5cb5e-9f58-452e-a661-a0258d7fb512.png)

![sectors-pie-chart](https://user-images.githubusercontent.com/11336712/120564463-a5cc8980-c413-11eb-8326-46efcdc85c23.gif)

All you need to do is download the latest broker reports or [enter manually](src/main/asciidoc/investbook-forms.adoc)
information. In this case, all information is saved on your computer, the data does not go to the cloud, and the 
Internet is not required to work.

For each account separately and summing up a single total for all accounts, the following information will be available:
- [review](src/main/asciidoc/portfolio-analysis.adoc) of asset growth calculated using the S&P 500 method,
  compared to the S&P 500, investment history and cash balances;  
  ![portfolio-analysis](https://user-images.githubusercontent.com/11336712/102415874-fd17a280-4009-11eb-9bff-232975adf21b.png)
  <img src="https://user-images.githubusercontent.com/11336712/102416414-d4dc7380-400a-11eb-95b1-8ff8ae37bd17.png" width="32%"/>
  <img src="https://user-images.githubusercontent.com/11336712/149419132-cad11fc3-fdaa-4572-882b-4ed49b937afe.png" width="32%"/>
  <img src="https://user-images.githubusercontent.com/11336712/102419341-9a75d500-4010-11eb-817a-a9b322237dd2.png" width="32%"/>
- [portfolio](src/main/asciidoc/portfolio-status.adoc) of securities with information about the current position, 
  average price purchases and yield of securities (XIRR), taking into account hedging positions in the derivatives 
  market and the average purchase price of currency;  
  ![portfolio](https://user-images.githubusercontent.com/11336712/104820094-af2dce80-5843-11eb-8083-6521ea537334.png)
- share of a security in a [portfolio](src/main/asciidoc/portfolio-status.adoc);  
  ![current-proportion](https://user-images.githubusercontent.com/11336712/88717010-8cd6b600-d128-11ea-901f-2b3fcee96f07.png)
- [trader's portfolio](src/main/asciidoc/derivatives-market-total-profit.adoc) with information on profitability transactions on the derivatives market in the context 
  of a group of contracts (for example, for all futures and options Si, the same for BR, etc.);  
  ![derivatives-marker-total-profit](https://user-images.githubusercontent.com/11336712/119887746-30f1df00-bf3d-11eb-9c52-713093ae4d72.png)
- distribution of profit across groups of derivatives contracts in the [trader’s portfolio](src/main/asciidoc/derivatives-market-total-profit.adoc);  
  ![derivatives-profit-proportion](https://user-images.githubusercontent.com/11336712/120565530-fb099a80-c415-11eb-82bb-8288ed9b7806.png)
- details of dividend, coupon and depreciation [payments](src/main/asciidoc/portfolio-payment.adoc);  
  ![portfolio-payment](https://user-images.githubusercontent.com/11336712/88460806-93a2c600-cea7-11ea-8ac9-95406fd6cec8.png)
- details of dividend, coupon and depreciation [payments](src/main/asciidoc/foreign-portfolio-payment.adoc),
  accrued on shares and bonds from the linked IIS account;
  ![foreign-portfolio-payment](https://user-images.githubusercontent.com/11336712/87988115-7907d000-cae8-11ea-9ec7-d56a120aac89.png)
- profitability of transactions on the [stock](src/main/asciidoc/stock-market-profit.adoc) market (FIFO method);  
  ![stock-market](https://user-images.githubusercontent.com/11336712/78156498-8de02b00-7447-11ea-833c-cfc755bd7558.png)
- profitability of transactions on the [derivatives](src/main/asciidoc/derivatives-market-profit.adoc) market;  
  ![derivatives-market](https://user-images.githubusercontent.com/11336712/78156504-8f115800-7447-11ea-87e5-3cd4c34aab47.png)
- profitability of transactions on the [foreign exchange](src/main/asciidoc/foreign-market-profit.adoc) market;  
  ![foreign-market](https://user-images.githubusercontent.com/11336712/84881751-fa59e600-b096-11ea-8b83-19d1c1229d73.png)
- [input and output](src/main/asciidoc/securities-deposit-and-withdrawal.adoc) of securities from/to other accounts, conversion, split of shares (AAPL, TSLA, etc.);  
  <img src="https://user-images.githubusercontent.com/11336712/87883425-f3185600-ca0f-11ea-9677-4689aa6a4ee5.png" width="40%"/>
- [profitability](src/main/asciidoc/cash-flow.adoc) of portfolio (XIRR), replenishments, write-offs, transfers from/to other accounts, 
- current cash balance;  
  ![cash-in](https://user-images.githubusercontent.com/11336712/100395491-3172f100-3052-11eb-9652-cd5730ac2e6f.png)
- [tax](src/main/asciidoc/tax.adoc) burden, including the
  [obligation](src/main/asciidoc/stock-market-profit.adoc#tax-liability) to independently pay tax for foreign securities;  
  ![tax](https://user-images.githubusercontent.com/11336712/96353102-b83ac280-10d1-11eb-9024-b0de4f4b153e.png)
- [broker](src/main/asciidoc/commission.adoc) commission.  
  <img src="https://user-images.githubusercontent.com/11336712/92284436-a1b61e80-ef0a-11ea-9eed-9a948089bcff.png" width="65%"/>

### Difference from similar products
Investbook in comparison with [Intelinvest](https://intelinvest.ru) and [Snowball Income](https://snowball-income.com)
has the following advantages:

1. No monthly subscription. For free. Always and unconditionally.
2. Dividends, coupons and tax payments are taken into account upon receipt. This allows you to accurately calculate
   profitability, timely track delays in receipt of payments to the account.
3. High accuracy of portfolio value assessment and tax liabilities. Unlike analogues of the amount and date of receipt
   payments specified in the application can be directly used to draw up a 3-NDFL declaration.
4. No Internet access required, the ability to work in your free time while traveling without the Internet.
5. Safety. No need to upload broker reports to the cloud, no need to provide an access token
   to the brokerage account to third parties and software with potential errors. For example, it is known
   that tokens of a popular broker allow you to make transactions without your participation.
6. Open source is an additional guarantee of security and confidence in the availability of data only to you.
7. Clear, widely used report format - Excel tables with [detailed description](src/main/asciidoc/investbook-report.adoc)
   of each column.
8. Unified data presentation format "[Portfolio Open Format](https://github.com/spacious-team/portfolio-open-format)"
   if necessary, it will allow you to painlessly transfer the accumulated data to another investment accounting application.

### Brokers
The application analyzes reports from brokers TBank / Tinkoff (xlsx), Sberbank (xlsx), VTB (xls), Promsvyazbank (xlsx, xml)
and Your Broker / Uralsib (zip with xls). If your account is opened with another broker,
write to [us](https://t.me/investbook_support). You can also use it already on your version of the application
[forms](src/main/asciidoc/investbook-forms.adoc) for entering information or
[download](src/main/asciidoc/investbook-input-format.adoc) data from Excel file. Your broker's support can also be
offered to third-party developers through extension functionality. Instructions for installing extensions are available for
operating systems [windows](docs/install-on-windows.md), [mac](docs/install-on-linux.md) and [linux](docs/install-on-linux.md).

### Install
Download the `.msi` installer from the [project] page (https://github.com/spacious-team/investbook/releases/latest)
and run it.

You can refer to more detailed instructions for installing and using the application for operating systems
[windows](docs/install-on-windows.md), [mac](docs/install-on-linux.md) and [linux](docs/install-on-linux.md).
Investbook can also be run in [docker](docs/run-by-docker.md).

### Working with the application
Launch the application via a shortcut on the Windows desktop, in the browser go to http://localhost:2030
and download broker reports (from your local computer or email).

For convenience, the application allows:
1. Downloading the same report multiple times (useful if you don't remember whether you downloaded a particular 
   report or not), There will be no data duplication.
2. Downloading reports for any time interval (day, month, year, etc.), and it is acceptable that reports of different 
   time periods will overlap.
3. It is acceptable to download reports for multiple brokerage/investment accounts, including from different brokerage 
   houses.

After downloading the report, analytical download in [excel file](src/main/asciidoc/investbook-report.adoc) 
format becomes available.

### Application update
The update process on Windows is no different from the initial installation process. Use the instructions
for operating systems [windows](docs/install-on-windows.md), [mac](docs/install-on-linux.md) or
[linux](docs/install-on-linux.md). Or, if Investbook was run in docker, use
[instructions](docs/run-by-docker.md).

### Documentation
Additional information can be found in [documentation](docs/documentation.md), also offline documentation is always 
available is available to you in the installed application on the main page via the "Documentation" link.

### License
The application is free (you are allowed to use, distribute, copy and make changes).
The license text is available in [English](https://www.gnu.org/licenses/agpl-3.0.html) and
[Russian](http://antirao.ru/gpltrans/agplru.pdf) languages, and also available 
[explanation](https://www.gnu.org/licenses/quick-guide-gplv3.html)
and answers to [questions](https://www.gnu.org/licenses/gpl-faq.ru.html) in Russian.

The license implies that the application is released to the public. Application version
from [page](https://github.com/spacious-team/investbook/releases) will always be distributed free of charge. 
But the license also allows any developer to improve their own copy of the application, including for the purpose of
[sales](https://www.gnu.org/licenses/gpl-faq.ru.html#DoesTheGPLAllowMoney) (with the caveat that the modified source code
will be open on the Internet).

### Why is the application code open source
For some areas, open source solutions are better suited than others, for example in the fields of finance and data
encryption, because these solutions can be trusted because you or anyone else can look at the code and see
in the security of the program. Many famous brands use open source, [Instagram](https://github.com/Instagram) for example,
[Android](https://ru.wikipedia.org/wiki/Android#%D0%98%D1%81%D1%85%D0%BE%D0%B4%D0%BD%D1%8B%D0%B9_%D0%BA%D0%BE%D0%B4),
[Telegram](https://ru.wikipedia.org/wiki/Telegram), [Twitter](https://opensource.twitter.dev/),
[Google Chrome](https://ru.wikipedia.org/wiki/Google_Chrome),
[Mozilla Firefox](https://developer.mozilla.org/en-US/docs/Mozilla/Developer_guide/Source_Code/Downloading_Source_Archives),
sites with a secure [https](https://ru.wikipedia.org/wiki/OpenSSL) connection, such as https://vk.com, etc.
The idea of open source is the freedom to develop and use software.

<details>
<summary>Elon Musk's opinion on open source.</summary>

> We will publish more source code and make it publicly available. And of course he will also be criticized,
people will help you find all the stupid things in the code. And we will quickly correct them, and we will do it under 
full public control. I think this approach will allow us to gain public trust. Because here you don't have to take my 
word for it, you can read the code with your own eyes, and what people say about this code. And you can see the 
improvements we're making. You can monitor the entire process in real time and see all the improvements. I'd be surprised
If only society hadn't thought after that, "Wow, this looks like something we can trust!" Well, it's true, this story
should inspire much more trust than others with all their black boxes and refusal to show the inside story.
What are you trying to hide? Clearly not something good. If you have nothing to hide, why not show it?
>
> [_2023 year interview_](https://www.youtube.com/watch?v=bOznEZAjX3I&t=5138s)
</details>

### How to help
You can help by expanding or correcting the [documentation](https://github.com/spacious-team/investbook/files/5398264/github.docx),
[reporting](https://github.com/spacious-team/investbook/issues/new/choose)  problems with the application,
[offering](https://github.com/spacious-team/investbook/issues/new/choose) new functionality or improving the Investbook application code.

There is also [extension](/docs/extension-developer-guide.md) functionality, that allows third-party developers
expand the list of [brokers](#brokers) supported out of the box. Extensions can be connected at the request of users
to the application. Third-party developers can distribute extensions for free or for a [fee](https://youtu.be/q4O6PX0ZuFU),
therefore, developers, even pursuing different goals, work together. If you decide to improve the application in this 
repository, please read the following [information](docs/CONTRIBUTING.md).

### Contacts
- Telegram [channel](https://t.me/investbook_official), technical [support](https://t.me/investbook_support_bot)
  and users [chat](https://t.me/investbook_support);
- Discussion on the forum [banki.ru](https://www.banki.ru/forum/?PAGE_NAME=read&FID=21&TID=380178);
- Application page on the [smart-lab.ru](https://smart-lab.ru/trading-software/Investbook) and
  [contact page](https://smart-lab.ru/profile/SpaciousTeam);
- e-mail: [spacious-team@ya.ru](mailto:spacious-team@ya.ru).

You can leave your review on the website [otzovik.com](https://otzovik.com/reviews/investbook-prilozhenie_investora_i_treydera/).

Evaluate investment performance easily and confidentially.

<img src="https://user-images.githubusercontent.com/11336712/85948991-b13e4780-b95c-11ea-9df6-a28be74c489d.png" width="100%"/>

package ru.portfolio.portfolio.view.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockMarketExcelProfitTableHeader implements ExcelProfitTableHeader {
    SECURITY("Бумага"),
    BUY_DATE("Дата покупки"),
    COUNT("Количество"),
    BUY_PRICE("Цена, руб"),
    BUY_AMOUNT("Стоимость (без НКД и комиссии)"),
    BUY_ACCRUED_INTEREST("НКД уплоченный"),
    BUY_COMMISSION("Комиссия покупки"),
    CELL_DATE("Дата продажи"),
    CELL_AMOUNT("Стоимость продажи/погашения"),
    CELL_ACCRUED_INTEREST("НКД при продаже" ),
    COUPON("Выплачены купоны"),
    AMORTIZATION("Амортизация облигации"),
    DIVIDEND("Дивиденды"),
    CELL_COMMISSION("Комиссия продажи/погашения"),
    TAX("Налог с купонов и дивидендов (уплаченный)"),
    FORECAST_TAX("Налог с разницы курсов (ожидаемый)"),
    PROFIT("Доходность годовых, %");

    private final String description;
}

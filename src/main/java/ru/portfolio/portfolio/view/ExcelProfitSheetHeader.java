package ru.portfolio.portfolio.view;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExcelProfitSheetHeader {
    SECURITY("Бумага"),
    BUY_DATE("Дата покупки"),
    COUNT("Количество"),
    BUY_PRICE("Цена, % номинала/руб"),
    BUY_AMOUNT("Стоимость (без НКД и комиссии)"),
    BUY_ACCRUED_INTEREST("НКД уплоченный"),
    BUY_COMMISSION("Комиссия покупки"),
    CELL_DATE("Дата продажи"),
    CELL_AMOUNT("Стоимость прод/погаш"),
    CELL_ACCRUED_INTEREST("НКД при продаже" ),
    COUPON("Выплачены купоны"),
    AMORTIZATION("Амортизация облигации"),
    DIVIDEND("Дивиденды"),
    CELL_COMMISSION("Комиссия продажи/погашения"),
    TAX("Налог с купонов и дивидендов"),
    FORECAST_TAX("Налог (с разницы курсов)"),
    PROFIT("Доходность годовых, %");

    static final String ROW_NUM_PLACE_HOLDER = "{rowNum}";
    private final String description;

    String getCellAddr() {
        return getColumnIndex() + ROW_NUM_PLACE_HOLDER;
    }

    char getColumnIndex() {
        return  (char) ('A' + this.ordinal());
    }
}

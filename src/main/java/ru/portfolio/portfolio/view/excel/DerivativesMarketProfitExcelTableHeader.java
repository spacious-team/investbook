package ru.portfolio.portfolio.view.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DerivativesMarketProfitExcelTableHeader implements ExcelTableHeader {
    CONTRACT("Контракт"),
    DIRECTION("Сделка"),
    DATE("Дата"),
    COUNT("Количество"),
    QUOTE("Котировка, пункты"),
    AMOUNT("Сумма, руб"),
    COMMISSION("Коммиссия, руб"),
    DERIVATIVE_PROFIT_DAY("Дневная вариационная маржа, руб"),
    DERIVATIVE_PROFIT_TOTAL("Накопленная вариационная маржа, руб"),
    POSITION("Позиция на конец дня"),
    FORECAST_TAX("Ожидаемый налог, руб"),
    PROFIT("Прибыль, руб");

    private final String description;
}

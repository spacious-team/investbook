package ru.portfolio.portfolio.view.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DerivativesMarketExcelProfitTableHeader implements ExcelProfitTableHeader {
    CONTRACT("Контракт"),
    OPEN_DATE("Дата открытия"),
    DIRECTION("Направление сделки открытия"),
    COUNT("Количество"),
    OPEN_QUOTE("Котировка открытия, пункты"),
    OPEN_COMMISSION("Комиссия открытия, руб"),
    CLOSE_DATE("Дата закрытия"),
    CLOSE_QUOTE("Котировака закрытия, пункты"),
    PAYMENT("Вариационная маржа полученная, руб"),
    CLOSE_COMMISSION("Коммисия закрытия, руб"),
    FORECAST_TAX("Ожидаемый налог от сделки, руб"),
    PROFIT("Доход, руб");

    private final String description;
}

package ru.portfolio.portfolio.view.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CashFlowExcelTableHeader implements ExcelTableHeader {
    DATE("Дата"),
    CASH("Внесено (+)/Выведено (-), руб"),
    CURRENCY("Валюта"),
    DAYS_COUNT("Количество дней на счету"),
    DESCRIPTION("Описание"),
    LIQUIDATION_VALUE("Ликвидная стоимость, руб"),
    PROFIT("Доходность годовых (простая), %");

    private final String description;
}

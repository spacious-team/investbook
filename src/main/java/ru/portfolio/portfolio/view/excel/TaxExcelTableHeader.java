package ru.portfolio.portfolio.view.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaxExcelTableHeader implements ExcelTableHeader {
    DATE("Дата"),
    TAX("Налог"),
    CURRENCY("Валюта"),
    DESCRIPTION("Описание");

    private final String description;
}

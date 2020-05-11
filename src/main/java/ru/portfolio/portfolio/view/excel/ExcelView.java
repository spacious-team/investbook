/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.portfolio.portfolio.view.excel;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExcelView {
    private final StockMarketProfitExcelTableView stockMarketProfitExcelTableView;
    private final DerivativesMarketProfitExcelTableView derivativesMarketProfitExcelTableView;
    private final ForeignMarketProfitExcelTableView foreignMarketProfitExcelTableView;
    private final ForeignPortfolioPaymentTableView foreignPortfolioPaymentTableView;
    private final CashFlowExcelTableView cashFlowExcelTableView;
    private final TaxExcelTableView taxExcelTableView;

    public void writeTo(XSSFWorkbook book) {
        CellStyles styles = new CellStyles(book);
        stockMarketProfitExcelTableView.writeTo(book, styles, portfolio -> portfolio + " (фондовый)");
        derivativesMarketProfitExcelTableView.writeTo(book, styles, portfolio -> portfolio + " (срочный)");
        foreignMarketProfitExcelTableView.writeTo(book, styles, portfolio -> portfolio + " (валюта)");
        foreignPortfolioPaymentTableView.writeTo(book, styles, portfolio -> portfolio + " (внешние выплаты)");
        cashFlowExcelTableView.writeTo(book, styles, portfolio -> "Доходность (" + portfolio + ")");
        taxExcelTableView.writeTo(book, styles, portfolio -> "Налог (" + portfolio + ")");
        if (book.getNumberOfSheets() == 0) {
            book.createSheet("пустой отчет");
        }
    }
}

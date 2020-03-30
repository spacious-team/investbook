package ru.portfolio.portfolio.view.excel;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExcelView {
    private final StockMarketProfitExcelTableView stockMarketProfitExcelTableView;
    private final DerivativesMarketProfitExcelTableView derivativesMarketProfitExcelTableView;

    public void writeTo(XSSFWorkbook book) {
        CellStyles styles = new CellStyles(book);
        stockMarketProfitExcelTableView.writeTo(book, styles, portfolio -> portfolio + " (фондовый)");
        derivativesMarketProfitExcelTableView.writeTo(book, styles, portfolio -> portfolio + " (срочный)");
    }
}

package ru.portfolio.portfolio.view.excel;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfitExcelView {
    private final StockMarketExcelProfitTable stockMarketExcelProfitTable;
    private final DerivativesMarketExcelProfitTable derivativesMarketExcelProfitTable;

    public void writeTo(XSSFWorkbook book) {
        CellStyles styles = new CellStyles(book);
        stockMarketExcelProfitTable.writeTo(book, styles, portfolio -> portfolio + " (фондовый)");
        derivativesMarketExcelProfitTable.writeTo(book, styles, portfolio -> portfolio + " (срочный)");
    }
}

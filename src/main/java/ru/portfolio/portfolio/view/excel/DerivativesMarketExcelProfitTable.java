package ru.portfolio.portfolio.view.excel;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.repository.PortfolioRepository;
import ru.portfolio.portfolio.view.ProfitTable;

import java.util.List;

import static ru.portfolio.portfolio.view.excel.DerivativesMarketExcelProfitTableHeader.*;

@Component
@RequiredArgsConstructor
public class DerivativesMarketExcelProfitTable extends ExcelProfitTable {
    private final PortfolioRepository portfolioRepository;
    private final DerivativesMarketExcelProfitTableFactory derivativesMarketExcelProfitTableFactory;

    @Override
    protected List<PortfolioEntity> getPortfolios() {
        // TODO select by user
        return portfolioRepository.findAll();
    }

    @Override
    protected ProfitTable getProfitTable(PortfolioEntity portfolio) {
        return derivativesMarketExcelProfitTableFactory.create(portfolio);
    }

    @Override
    protected void writeHeader(Sheet sheet, CellStyle style) {
        super.writeHeader(sheet, style);
        //sheet.setColumnWidth(StockMarketProfitExcelTableHeader.SECURITY.ordinal(), 45 * 256);
    }

    @Override
    protected ProfitTable.Record getTotalRow() {
        ProfitTable.Record totalRow = new ProfitTable.Record();
        for (DerivativesMarketExcelProfitTableHeader column : DerivativesMarketExcelProfitTableHeader.values()) {
            totalRow.put(column, "=SUM(" +
                    column.getColumnIndex() + "3:" +
                    column.getColumnIndex() + "100000)");
        }
        totalRow.put(CONTRACT, "Итого:");
        totalRow.remove(OPEN_DATE);
        totalRow.remove(CLOSE_DATE);
        totalRow.remove(OPEN_QUOTE);
        totalRow.remove(CLOSE_QUOTE);
        return totalRow;
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, CellStyles styles) {
/*        for (Cell cell : sheet.getRow(1)) {
            if (cell.getColumnIndex() == StockMarketProfitExcelTableHeader.SECURITY.ordinal()) {
                cell.setCellStyle(styles.getTotalTextStyle());
            } else {
                cell.setCellStyle(styles.getTotalRowStyle());
            }
        }
        for (Row row : sheet) {
            Cell cell = row.getCell(StockMarketProfitExcelTableHeader.SECURITY.ordinal());
            cell.setCellStyle(styles.getSecurityNameStyle());
            cell = row.getCell(StockMarketProfitExcelTableHeader.COUNT.ordinal());
            cell.setCellStyle(styles.getIntStyle());
        }*/
    }
}

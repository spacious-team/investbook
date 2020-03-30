package ru.portfolio.portfolio.view.excel;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.repository.PortfolioRepository;
import ru.portfolio.portfolio.view.Table;
import ru.portfolio.portfolio.view.TableHeader;

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
    protected Table getProfitTable(PortfolioEntity portfolio) {
        return derivativesMarketExcelProfitTableFactory.create(portfolio);
    }

    @Override
    protected void writeHeader(Sheet sheet, Class<? extends TableHeader> headerType, CellStyle style) {
        super.writeHeader(sheet, headerType, style);
        sheet.setColumnWidth(CONTRACT.ordinal(), 24 * 256);
        sheet.setColumnWidth(AMOUNT.ordinal(), 16 * 256);
    }

    @Override
    protected Table.Record getTotalRow() {
        Table.Record totalRow = new Table.Record();
        totalRow.put(CONTRACT, "Итого:");
        totalRow.put(COUNT, getSumFormula(COUNT));
        totalRow.put(AMOUNT, "=SUMPRODUCT(ABS(" +
                AMOUNT.getColumnIndex() + "3:" +
                AMOUNT.getColumnIndex() + "100000))");
        totalRow.put(COMMISSION, getSumFormula(COMMISSION) + "/2");
        totalRow.put(DERIVATIVE_PROFIT_DAY, getSumFormula(DERIVATIVE_PROFIT_DAY));
        totalRow.put(FORECAST_TAX, getSumFormula(FORECAST_TAX));
        totalRow.put(PROFIT, getSumFormula(PROFIT));
        return totalRow;
    }

    private String getSumFormula(DerivativesMarketExcelProfitTableHeader column) {
        return "=SUM(" +
                column.getColumnIndex() + "3:" +
                column.getColumnIndex() + "100000)";
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, CellStyles styles) {
        super.sheetPostCreate(sheet, styles);
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell = row.getCell(CONTRACT.ordinal());
            if (cell != null) {
                cell.setCellStyle(styles.getSecurityNameStyle());
            }
        }
        for (Cell cell : sheet.getRow(1)) {
            if (cell == null) continue;
            if (cell.getColumnIndex() == CONTRACT.ordinal()) {
                cell.setCellStyle(styles.getTotalTextStyle());
            } else if (cell.getColumnIndex() == COUNT.ordinal()){
                cell.setCellStyle(styles.getIntStyle());
            } else {
                cell.setCellStyle(styles.getTotalRowStyle());
            }
        }
    }
}

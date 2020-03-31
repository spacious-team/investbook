package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellAddress;
import ru.portfolio.portfolio.parser.BrokerReport;
import ru.portfolio.portfolio.parser.ExcelTable;
import ru.portfolio.portfolio.parser.ExcelTableHelper;
import ru.portfolio.portfolio.parser.ReportTable;
import ru.portfolio.portfolio.pojo.PortfolioPropertyType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PortfolioPropertyTable implements ReportTable<PortfolioPropertyTable.PortfolioPropertyRow> {
    private static final String ASSETS = "\"СУММА АКТИВОВ\" на конец дня";
    @Getter
    private final BrokerReport report;
    @Getter
    private final List<PortfolioPropertyTable.PortfolioPropertyRow> data = new ArrayList<>();


    protected PortfolioPropertyTable(PsbBrokerReport report) {
        this.report = report;
        this.data.addAll(getRow(report));
    }

    protected Collection<PortfolioPropertyRow> getRow(PsbBrokerReport report) {
        CellAddress address = ExcelTableHelper.find(report.getSheet(), ASSETS);
        if (address == ExcelTableHelper.NOT_FOUND) {
            return Collections.emptyList();
        }
        CellAddress assestsAddr = ExcelTableHelper.findByPredicate(
                report.getSheet(),
                address.getRow(),
                cell -> cell.getCellType() == CellType.NUMERIC);
        if (assestsAddr == ExcelTableHelper.NOT_FOUND) {
            return Collections.emptyList();
        }

        return Collections.singletonList(
                PortfolioPropertyRow.builder()
                .property(PortfolioPropertyType.TOTAL_ASSETS)
                .value(ExcelTable.getCurrencyCellValue(ExcelTableHelper.getCell(report.getSheet(), assestsAddr)))
                .build());
    }

    @Getter
    @Builder
    public static class PortfolioPropertyRow {
        private final PortfolioPropertyType property;
        private final Object value;
    }
}

package ru.portfolio.portfolio.parser;

import lombok.Getter;
import org.apache.poi.ss.usermodel.Row;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractReportTable<RowType> implements ReportTable<RowType> {
    @Getter
    private final BrokerReport report;
    @Getter
    private final List<RowType> data = new ArrayList<>();

    protected AbstractReportTable(BrokerReport report,
                                  String tableName,
                                  String tableFooter,
                                  Class<? extends TableColumnDescription> headerDescription) {
        this.report = report;
        ExcelTable table = (tableFooter != null && !tableFooter.isEmpty()) ?
                ExcelTable.of(report.getSheet(), tableName, tableFooter, headerDescription) :
                ExcelTable.of(report.getSheet(), tableName, headerDescription);
        this.exelTableConfiguration(table);
        this.data.addAll(pasreTable(table));
    }

    protected void exelTableConfiguration(ExcelTable table) {
    }

    protected Collection<RowType> pasreTable(ExcelTable table) {
        return table.getDataCollection(getReport().getPath(), this::getRow);
    }

    protected Instant convertToInstant(String dateTime) {
        return report.convertToInstant(dateTime);
    }

    protected abstract Collection<RowType> getRow(ExcelTable table, Row row);
}

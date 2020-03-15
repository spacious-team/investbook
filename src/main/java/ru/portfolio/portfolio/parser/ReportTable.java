package ru.portfolio.portfolio.parser;

import java.util.List;

public interface ReportTable<RowType> {
    BrokerReport getReport();
    List<RowType> getData();
}

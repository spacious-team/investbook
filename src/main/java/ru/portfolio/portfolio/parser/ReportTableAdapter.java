package ru.portfolio.portfolio.parser;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ReportTableAdapter<RowType> implements ReportTable<RowType> {

    @Getter
    private final BrokerReport report;
    @Getter
    private final List<RowType> data = new ArrayList<>();

    protected ReportTableAdapter(ReportTable<RowType>... tables) {
        Set<BrokerReport> reports = Arrays.stream(tables).map(ReportTable::getReport).collect(Collectors.toSet());
        if (reports.size() == 1) {
            this.report = new ArrayList<>(reports).get(0);
        } else {
            throw new IllegalArgumentException("Can't wrap different report tables");
        }
        Arrays.asList(tables).forEach(r -> data.addAll(r.getData()));
    }
}

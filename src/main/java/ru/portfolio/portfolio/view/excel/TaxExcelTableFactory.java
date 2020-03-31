package ru.portfolio.portfolio.view.excel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.EventCashFlowConverter;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.EventCashFlow;
import ru.portfolio.portfolio.pojo.Portfolio;
import ru.portfolio.portfolio.repository.EventCashFlowRepository;
import ru.portfolio.portfolio.view.Table;
import ru.portfolio.portfolio.view.TableFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.portfolio.portfolio.view.excel.TaxExcelTableHeader.*;

@Component
@RequiredArgsConstructor
public class TaxExcelTableFactory implements TableFactory {
    private final EventCashFlowRepository eventCashFlowRepository;
    private final EventCashFlowConverter eventCashFlowConverter;

    @Override
    public Table create(Portfolio portfolio) {
        Table table = new Table();
        List<EventCashFlow> cashFlows = eventCashFlowRepository
                .findByPortfolioIdAndCashFlowTypeIdOrderByTimestamp(
                        portfolio.getId(),
                        CashFlowType.TAX.getId())
                .stream()
                .map(eventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));

        for (EventCashFlow cash : cashFlows) {
            Table.Record record = new Table.Record();
            record.put(DATE, cash.getTimestamp());
            record.put(TAX, Optional.ofNullable(cash.getValue())
                    .map(BigDecimal::abs)
                    .orElse(BigDecimal.ZERO));
            record.put(CURRENCY, cash.getCurrency());
            record.put(DESCRIPTION, cash.getDescription());
            table.add(record);
        }
        return table;
    }
}

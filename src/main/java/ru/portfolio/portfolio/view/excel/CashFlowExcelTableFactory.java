package ru.portfolio.portfolio.view.excel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.EventCashFlowEntityConverter;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.EventCashFlow;
import ru.portfolio.portfolio.repository.CashFlowTypeRepository;
import ru.portfolio.portfolio.repository.EventCashFlowRepository;
import ru.portfolio.portfolio.view.Table;
import ru.portfolio.portfolio.view.TableFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.portfolio.portfolio.view.excel.CashFlowExcelTableHeader.*;

@Component
@RequiredArgsConstructor
public class CashFlowExcelTableFactory implements TableFactory {
    // TODO DAYS() excel function not impl by Apache POI: https://bz.apache.org/bugzilla/show_bug.cgi?id=58468
    private static final String DAYS_COUNT_FORMULA = "=DAYS360(" + DATE.getCellAddr() + ",TODAY())";
    private final EventCashFlowRepository eventCashFlowRepository;
    private final CashFlowTypeRepository cashFlowTypeRepository;
    private final EventCashFlowEntityConverter eventCashFlowEntityConverter;

    @Override
    public Table create(PortfolioEntity portfolio) {
        Table table = new Table();
        List<EventCashFlow> cashFlows = eventCashFlowRepository
                .findByPortfolioAndCashFlowTypeOrderByTimestamp(
                        portfolio,
                        cashFlowTypeRepository.findById(CashFlowType.CASH.getType()).orElseThrow())
                .stream()
                .map(eventCashFlowEntityConverter::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));

        for (EventCashFlow cash : cashFlows) {
            Table.Record record = new Table.Record();
            record.put(DATE, cash.getTimestamp());
            record.put(CASH, cash.getValue());
            record.put(CURRENCY, cash.getCurrency());
            record.put(DAYS_COUNT, DAYS_COUNT_FORMULA);
            record.put(DESCRIPTION, cash.getDescription());
            table.add(record);
        }
        return table;
    }
}

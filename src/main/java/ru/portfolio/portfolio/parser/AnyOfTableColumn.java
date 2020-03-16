package ru.portfolio.portfolio.parser;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.poi.ss.usermodel.Row;

import java.util.Arrays;

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class AnyOfTableColumn implements TableColumn {

    private final TableColumn[] columns;

    public static TableColumn of(TableColumn... columns) {
        return new AnyOfTableColumn(columns);
    }

    @Override
    public int getColumnIndex(Row header) {
        for (TableColumn c : columns) {
            try {
                return c.getColumnIndex(header);
            } catch (RuntimeException ignore){
            }
        }
        throw new RuntimeException("Не обнаружен заголовок таблицы, включающий: " + String.join(", ",
                Arrays.stream(columns)
                        .map(TableColumn::toString)
                        .toArray(String[]::new)));
    }
}

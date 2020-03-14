package ru.portfolio.portfolio.parser;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.util.Arrays;

@ToString
@EqualsAndHashCode
public class TableColumn {
    private final String[] words;

    public static TableColumn of(String... words) {
        return new TableColumn(words);
    }

    private TableColumn(String... words) {
        this.words = Arrays.stream(words)
                .map(String::toLowerCase)
                .toArray(String[]::new);
    }

    public int getColumnIndex(Row header) {
        next_cell:
        for (Cell cell : header) {
            if (cell.getCellType() == CellType.STRING) {
                String colName = cell.getStringCellValue();
                if (colName != null) {
                    colName = colName.toLowerCase();
                    for (String word : words) {
                        if (!containsWord(colName, word)) {
                            continue next_cell;
                        }
                    }
                    return cell.getColumnIndex();
                }
            }
        }
        throw new RuntimeException("Не обнаружен заголовок таблицы, включающий слова: " + String.join(", ", words));
    }

    private boolean containsWord(String text, String word) {
        return text.matches("(^|.*\\b|.*\\s)" + word + "(\\b.*|\\s.*|$)");
    }
}

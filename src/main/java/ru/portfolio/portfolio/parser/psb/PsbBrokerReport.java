package ru.portfolio.portfolio.parser.psb;

import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.BiPredicate;

public class PsbBrokerReport implements AutoCloseable {
    public static final CellRangeAddress EMTPY_RANGE = new CellRangeAddress(-1, -1, -1, -1);
    public static final CellAddress NOT_ADDRESS = new CellAddress(-1, -1);

    private final XSSFWorkbook book;
    @Getter
    private final XSSFSheet sheet;

    public PsbBrokerReport(String exelFileName) throws IOException {
        this.book = new XSSFWorkbook(new FileInputStream(exelFileName));
        this.sheet = book.getSheetAt(0);
    }

    public CellAddress find(Object value) {
        return find(value, 0);
    }

    public CellAddress find(Object value, int startRow) {
        return find(value, startRow, sheet.getLastRowNum());
    }

    /**
     * @param startRow search rows start from this
     * @param endRow search rows excluding this
     */
    public CellAddress find(Object value, int startRow, int endRow) {
        return find(value, startRow, endRow, String::equals);
    }

    /**
     * @param stringPredicate cell and value comparing bi-predicate if cell value type is string
     */
    public CellAddress find(Object value, int startRow, int endRow, BiPredicate<String, Object> stringPredicate) {
        if (sheet.getLastRowNum() == -1) {
            return NOT_ADDRESS;
        } else if (endRow > sheet.getLastRowNum()) {
            endRow = sheet.getLastRowNum();
        }
        CellType type = getType(value);
        if (type == CellType.NUMERIC) {
            value = ((Number) value).doubleValue();
        }
        for(int rowNum = startRow; rowNum < endRow; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;
            for (Cell cell : row) {
                if (cell != null && cell.getCellType() == type) {
                    if (compare(value, cell, stringPredicate)) {
                        return cell.getAddress();
                    }
                }
            }
        }
        return NOT_ADDRESS;
    }

    private CellType getType(Object value) {
        CellType type;
        if (value instanceof String) {
            type = (((String) value).isEmpty()) ? CellType.BLANK : CellType.STRING;
        } else if (value instanceof Number) {
            type = CellType.NUMERIC;
        } else if (value instanceof Boolean) {
            type = CellType.BOOLEAN;
        } else if (value == null) {
            type = CellType.BLANK;
        } else {
            throw new IllegalArgumentException("Не могу сравнить значение '" + value + "' типа " + value.getClass().getName());
        }
        return type;
    }

    private boolean compare(Object value, Cell cell, BiPredicate<String, Object> stringPredicate) {
        switch (cell.getCellType()) {
            case BLANK:
                if (value == null || value.equals("")) return true;
                return false;
            case STRING:
                if (stringPredicate.test(cell.getStringCellValue(), value)) return true;
                return false;
            case NUMERIC:
                if (value instanceof Number && cell.getNumericCellValue() == ((Number) value).doubleValue()) return true;
                return false;
            case BOOLEAN:
                if (value.equals(cell.getBooleanCellValue())) return true;
                return false;
        }
        return false;
    }

    public boolean rowContains(int rowNum, Object value) {
        return find(value, rowNum, rowNum + 1, String::equals) != NOT_ADDRESS;
    }

    @Override
    public void close() throws Exception {
        this.book.close();
    }
}

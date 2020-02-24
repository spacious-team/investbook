package ru.portfolio.portfolio.parser.psb;

import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.BiPredicate;

public class PsbBrokerReport implements AutoCloseable {
    static final CellRangeAddress EMTPY_RANGE = new CellRangeAddress(-1, -1, -1, -1);
    private static final CellAddress NOT_ADDRESS = new CellAddress(-1, -1);
    private static final ZoneId zoneId = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final XSSFWorkbook book;
    @Getter
    private final XSSFSheet sheet;
    @Getter
    private final String portfolio;

    public PsbBrokerReport(String exelFileName) throws IOException {
        this(Paths.get(exelFileName));
    }

    public PsbBrokerReport(Path exelFileName) throws IOException {
        this.book = new XSSFWorkbook(Files.newInputStream(exelFileName));
        this.sheet = book.getSheetAt(0);
        this.portfolio = getPortfolio(this.sheet);
    }

    private static String getPortfolio(XSSFSheet sheet) {
        try {
            return sheet.getRow(9).getCell(3).getStringCellValue().split("/")[0];
        } catch (Exception e) {
            throw new RuntimeException("Ошибка поиска номера Брокерского счета в отчете");
        }
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

    public static Instant convertToInstant(String value) {
        if (value.contains(":")) {
            return LocalDateTime.parse(value, PsbBrokerReport.dateTimeFormatter).atZone(PsbBrokerReport.zoneId).toInstant();
        } else {
            return LocalDate.parse(value, PsbBrokerReport.dateFormatter).atStartOfDay(PsbBrokerReport.zoneId).toInstant();
        }
    }

    public CellRangeAddress getTableCellRange(String tableName, String tableFooterString) {
        CellAddress startAddress = find(tableName);
        if (startAddress.equals(NOT_ADDRESS)) {
            return EMTPY_RANGE;
        }
        CellAddress endAddress = find(tableFooterString, startAddress.getRow() + 2,
                getSheet().getLastRowNum(), (cell , prefix) -> cell.startsWith(prefix.toString()));
        if (endAddress.equals(NOT_ADDRESS)) {
            return EMTPY_RANGE;
        }
        return new CellRangeAddress(
                startAddress.getRow(),
                endAddress.getRow(),
                getSheet().getRow(startAddress.getRow()).getFirstCellNum(),
                getSheet().getRow(endAddress.getRow()).getLastCellNum());
    }

    /**
     * Get table ragne, table ends with empty line
     */
    public CellRangeAddress getTableCellRange(String tableName) {
        CellAddress startAddress = find(tableName);
        if (startAddress.equals(NOT_ADDRESS)) {
            return EMTPY_RANGE;
        }
        int lastRowNum = startAddress.getRow() + 1;
        LAST_ROW:
        for(; lastRowNum < getSheet().getLastRowNum(); lastRowNum++) {
            Row row = sheet.getRow(lastRowNum);
            if (row == null || row.getLastCellNum() == 0) {
                break;
            }
            for (Cell cell : row) {
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    continue LAST_ROW;
                }
            }
            break; // is all row cells blank
        }
        lastRowNum--; // exclude last row from table
        if (lastRowNum < startAddress.getRow()) lastRowNum = startAddress.getRow();
        return new CellRangeAddress(
                startAddress.getRow(),
                lastRowNum,
                getSheet().getRow(startAddress.getRow()).getFirstCellNum(),
                getSheet().getRow(lastRowNum).getLastCellNum());
    }

    @Override
    public void close() throws IOException {
        this.book.close();
    }
}

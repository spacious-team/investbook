package ru.portfolio.portfolio.view.excel;

import ru.portfolio.portfolio.view.ProfitTableHeader;

public interface ExcelProfitTableHeader extends ProfitTableHeader {
    String ROW_NUM_PLACE_HOLDER = "{rowNum}";

    default String getCellAddr() {
        return getColumnIndex() + ROW_NUM_PLACE_HOLDER;
    }

    default char getColumnIndex() {
        return  (char) ('A' + this.ordinal());
    }
}

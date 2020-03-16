package ru.portfolio.portfolio.parser;

import org.apache.poi.ss.usermodel.Row;

public interface TableColumn {

    int getColumnIndex(Row header);
}

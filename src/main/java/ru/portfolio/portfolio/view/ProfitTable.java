package ru.portfolio.portfolio.view;

import java.util.HashMap;
import java.util.LinkedList;

public class ProfitTable extends LinkedList<ProfitTable.Record> {

    public void addEmptyRecord() {
        add(Record.EMPTY);
    }

    public static class Record extends HashMap<ProfitTableHeader, Object> {
        public static Record EMPTY = new Record();

        public Record() {
        }

        public Record(Record record) {
            super(record);
        }
    }
}

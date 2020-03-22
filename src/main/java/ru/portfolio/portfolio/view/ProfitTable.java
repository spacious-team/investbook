package ru.portfolio.portfolio.view;

import java.util.HashMap;
import java.util.LinkedList;

public class ProfitTable extends LinkedList<ProfitTable.Record> {

    public static class Record extends HashMap<ProfitTableHeader, Object> {

        public Record() {
        }

        public Record(Record record) {
            super(record);
        }
    }
}

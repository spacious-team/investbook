package ru.portfolio.portfolio.view;

import java.util.HashMap;
import java.util.LinkedList;

public class Table extends LinkedList<Table.Record> {

    public void addEmptyRecord() {
        add(Record.EMPTY);
    }

    public static class Record extends HashMap<TableHeader, Object> {
        public static Record EMPTY = new Record();

        public Record() {
        }

        public Record(Record record) {
            super(record);
        }
    }
}

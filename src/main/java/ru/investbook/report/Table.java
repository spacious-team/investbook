/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.report;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.LinkedList;

public class Table extends LinkedList<Table.Record> {

    public void addEmptyRecord() {
        add(Record.EMPTY);
    }

    public Record addNewRecord() {
        Record record = new Record();
        add(record);
        return record;
    }

    public static Record newRecord() {
        return new Record();
    }

    public static class Record extends HashMap<TableHeader, @Nullable Object> {
        public static Record EMPTY = new Record();

        public Record() {
        }

        public Record(Record record) {
            super(record);
        }
    }
}

/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.view;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import ru.investbook.pojo.Transaction;

import java.time.Instant;

import static java.lang.Integer.signum;
import static java.lang.Math.abs;

@Getter
@ToString
@EqualsAndHashCode
public class OpenedPosition implements Position {
    private final Transaction openTransaction;
    /**
     * Positive for long position, negative for short position
     */
    private int unclosedPositions;

    OpenedPosition(Transaction openTransaction) {
        this.openTransaction = openTransaction;
        this.unclosedPositions = openTransaction.getCount();
    }

    OpenedPosition(Transaction openTransaction, int unclosedPositions) {
        this.openTransaction = openTransaction;
        this.unclosedPositions = unclosedPositions;
    }

    void closePositions(int count) {
        if (abs(count) > abs(this.unclosedPositions)) {
            throw new IllegalArgumentException("Недостаточно открытых бумаг в позиции " + openTransaction + " для закрытия " + count);
        } else if (signum(count) == signum(this.unclosedPositions)) {
            throw new IllegalArgumentException("Ожидается закрытие позиции " + openTransaction);
        } else if (count == 0) {
            throw new IllegalArgumentException("Ожидается закрытие позиции " + openTransaction);
        }
        this.unclosedPositions += count;
    }

    @Override
    public boolean wasOpenedAtTheInstant(Instant instant) {
        return openTransaction.getTimestamp().isBefore(instant);
    }

    @Override
    public boolean wasOpenedBetweenDates(Instant startDate, Instant endDate) {
        return startDate.isBefore(endDate) &&
                openTransaction.getTimestamp().isBefore(endDate);
    }

    @Override
    public int getCount() {
        return unclosedPositions;
    }
}

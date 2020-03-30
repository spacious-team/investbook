package ru.portfolio.portfolio.view;

import lombok.Getter;
import ru.portfolio.portfolio.pojo.Transaction;

import java.time.Instant;

import static java.lang.Integer.signum;
import static java.lang.Math.abs;

@Getter
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
    public int getCount() {
        return unclosedPositions;
    }
}

package ru.portfolio.portfolio.view;

import lombok.Getter;
import ru.portfolio.portfolio.pojo.Transaction;

import static java.lang.Integer.signum;
import static java.lang.Math.abs;

@Getter
class OpenedPosition {
    private final Transaction transaction;
    private int unclosedPositions;

    OpenedPosition(Transaction transaction) {
        this.transaction = transaction;
        this.unclosedPositions = transaction.getCount();
    }

    OpenedPosition(Transaction transaction, int unclosedPositions) {
        this.transaction = transaction;
        this.unclosedPositions = unclosedPositions;
    }

    void closePositions(int count) {
        if (abs(count) > abs(this.unclosedPositions)) {
            throw new IllegalArgumentException("Недостаточная позиция " + transaction + " для закрытия " + count + " бумаг");
        } else if (signum(count) == signum(this.unclosedPositions)) {
            throw new IllegalArgumentException("Ожидается закрытие позиции " + transaction);
        } else if (count == 0) {
            throw new IllegalArgumentException("Ожидается закрытие позиции " + transaction);
        }
        this.unclosedPositions += count;
    }
}

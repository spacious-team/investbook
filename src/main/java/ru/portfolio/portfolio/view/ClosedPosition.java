package ru.portfolio.portfolio.view;

import lombok.Getter;
import ru.portfolio.portfolio.pojo.Transaction;

@Getter
class ClosedPosition {
    private final Transaction openTransaction;
    private final Transaction closeTransaction;
    private final int count;

    ClosedPosition(Transaction openTransaction, Transaction closeTransaction, int count) {
        this.openTransaction = openTransaction;
        this.closeTransaction = closeTransaction;
        this.count = Math.abs(count);
    }
}

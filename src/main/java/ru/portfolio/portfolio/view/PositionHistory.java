package ru.portfolio.portfolio.view;

import lombok.Getter;
import ru.portfolio.portfolio.pojo.Transaction;

import java.time.Instant;

@Getter
class PositionHistory {
    private final Instant instant;
    /**
     * Opened positions after {@link #getInstant()}
     */
    private final int openedPositions;

    PositionHistory(Transaction transaction, int openedPositions) {
        this.instant = transaction.getTimestamp();
        this.openedPositions = openedPositions;
    }
}

package ru.portfolio.portfolio.view;

import lombok.Getter;
import ru.portfolio.portfolio.pojo.Transaction;

import java.time.Instant;

@Getter
class PastPosition {
    private final Instant instant;
    /**
     * Opened positions after {@link #getInstant()}
     */
    private final int openedPositions;

    PastPosition(Transaction transaction, int openedPositions) {
        this.instant = transaction.getTimestamp();
        this.openedPositions = openedPositions;
    }
}

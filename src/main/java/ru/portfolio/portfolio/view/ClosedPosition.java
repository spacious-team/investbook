package ru.portfolio.portfolio.view;

import lombok.Getter;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.Transaction;

import java.time.Instant;

@Getter
public class ClosedPosition extends OpenedPosition {
    private final Transaction closeTransaction;
    private final CashFlowType closingEvent;
    /**
     * Always positive value
     */
    private final int count;

    ClosedPosition(OpenedPosition opened, Transaction closing, int count, CashFlowType closingEvent) {
        super(opened.getOpenTransaction(), 0);
        this.closeTransaction = closing;
        this.count = count;
        this.closingEvent = closingEvent;
    }

    @Override
    public boolean wasOpenedAtTheInstant(Instant instant) {
        return getOpenTransaction().getTimestamp().isBefore(instant) &&
                instant.isBefore(getCloseTransaction().getTimestamp());
    }
}
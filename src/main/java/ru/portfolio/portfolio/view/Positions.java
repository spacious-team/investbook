package ru.portfolio.portfolio.view;

import lombok.Getter;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;
import ru.portfolio.portfolio.pojo.Transaction;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

import static java.lang.Integer.min;
import static java.lang.Integer.signum;
import static java.lang.Math.abs;

/**
 * Calculate {@link ClosedPosition}
 */
class Positions {
    @Getter
    Deque<PastPosition> pastPositions = new LinkedList<>();
    @Getter
    private final Deque<OpenedPosition> openedPositions = new LinkedList<>();
    @Getter
    private final Deque<ClosedPosition> closedPositions = new LinkedList<>();

    Positions(Deque<Transaction> transactions, Deque<SecurityEventCashFlow> redemptions) {
        updateSecuritiesPastPositions(transactions);
        for (Transaction transaction : transactions) {
            if (isIncreasePosition(transaction)) {
                this.openedPositions.add(new OpenedPosition(transaction));
            } else {
                closePositions(transaction, CashFlowType.PRICE);
            }
        }
        if (!redemptions.isEmpty() && (redemptions.peek() != null)) {
            String isin = redemptions.peek().getIsin();
            updateSecuritiesPastPositions(redemptions.stream()
                    .map(Positions::convertToTransaction)
                    .collect(Collectors.toCollection(LinkedList::new)));
            for (SecurityEventCashFlow  redemption : redemptions) {
                closePositions(convertToTransaction(redemption), CashFlowType.REDEMPTION);
            }
            if (!this.openedPositions.isEmpty() || this.pastPositions.getLast().getOpenedPositions() != 0) {
                throw new RuntimeException("Предоставлены не все транзакции по бумаге " +
                        isin + ", в истории портфеля есть событие погашения номинала облигаций по " +
                        redemptions.stream().mapToInt(SecurityEventCashFlow::getCount).sum() +
                        " бумагам, однако в портфеле остались " + this.pastPositions.getLast().getOpenedPositions() +
                        " открытые позиции");
            }
        }
    }

    private void updateSecuritiesPastPositions(Queue<Transaction> transactions) {
        int openedPosition = (!this.pastPositions.isEmpty()) ? this.pastPositions.peekLast().getOpenedPositions() : 0;
        for (Transaction transaction : transactions) {
            openedPosition += transaction.getCount();
            this.pastPositions.add(new PastPosition(transaction, openedPosition));
        }
    }

    private boolean isIncreasePosition(Transaction transaction) {
        OpenedPosition position = openedPositions.peek();
        return position == null || position.getUnclosedPositions() == 0 || (signum(transaction.getCount()) == signum(position.getUnclosedPositions()));
    }

    /**
     * @param closing position decreasing transaction
     */
    private void closePositions(Transaction closing, CashFlowType closingEvent) {
        int closingCount = abs(closing.getCount());
        while (!openedPositions.isEmpty() && closingCount > 0) {
            OpenedPosition opening = openedPositions.peek();
            int openedCount = abs(opening.getUnclosedPositions());
            if (openedCount <= closingCount) {
                openedPositions.remove();
            } else {
                opening.closePositions(closingCount * signum(closing.getCount()));
            }
            ClosedPosition closed = new ClosedPosition(opening, closing, min(openedCount, closingCount), closingEvent);
            closingCount -= closed.getCount();
            closedPositions.add(closed);
        }
        if (closingCount != 0) {
            openedPositions.add(new OpenedPosition(closing, closingCount));
        }
    }

    /**
     * Converts bonds redemption event to transaction
     */
    private static Transaction convertToTransaction(SecurityEventCashFlow redemption) {
        if (redemption.getEventType() != CashFlowType.REDEMPTION) {
            throw new IllegalArgumentException("Ожидается событие погашения номинала облигации, предоставлено событие "
                    + redemption.getEventType());
        }
        return Transaction.builder()
                .portfolio(redemption.getPortfolio())
                .isin(redemption.getIsin())
                .timestamp(redemption.getTimestamp())
                .count(-redemption.getCount())
                .build();
    }
}

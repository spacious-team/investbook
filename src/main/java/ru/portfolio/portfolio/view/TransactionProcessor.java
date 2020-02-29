package ru.portfolio.portfolio.view;

import lombok.Getter;
import ru.portfolio.portfolio.pojo.Transaction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import static java.lang.Integer.signum;
import static java.lang.Math.abs;

/**
 * Calculate {@link ClosedPosition}
 */
class TransactionProcessor {
    @Getter
    private final Queue<OpenedPosition> openedPositions = new LinkedList<>();
    @Getter
    private final ArrayList<ClosedPosition> closedPositions = new ArrayList<>();


    TransactionProcessor(ArrayList<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            if (isIncreasePosition(transaction)) {
                openedPositions.add(new OpenedPosition(transaction));
            } else {
                closePositions(transaction);
            }
        }
    }

    private boolean isIncreasePosition(Transaction transaction) {
        OpenedPosition position = openedPositions.peek();
        return position == null || position.getUnclosedPositions() == 0 || (signum(transaction.getCount()) == signum(position.getUnclosedPositions()));
    }

    /**
     * @param transaction position decreasing transaction
     */
    private void closePositions(Transaction transaction) {
        int closingCount = transaction.getCount();
        while (!openedPositions.isEmpty()) {
            OpenedPosition opening = openedPositions.peek();
            int openedCount = opening.getUnclosedPositions();
            if (abs(openedCount) == abs(closingCount)) {
                openedPositions.remove();
                closedPositions.add(new ClosedPosition(opening.getTransaction(), transaction, closingCount));
                return;
            } else if (abs(openedCount) > abs(closingCount)) {
                opening.closePositions(closingCount);
                closedPositions.add(new ClosedPosition(opening.getTransaction(), transaction, closingCount));
                return;
            } else {
                closingCount += openedCount;
                openedPositions.remove();
                closedPositions.add(new ClosedPosition(opening.getTransaction(), transaction, openedCount));
            }
        }
        if (closingCount != 0) {
            openedPositions.add(new OpenedPosition(transaction, closingCount));
        }
    }
}

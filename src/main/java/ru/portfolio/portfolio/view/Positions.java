/*
 * Portfolio
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
public class Positions {
    @Getter
    Deque<PositionHistory> positionHistories = new LinkedList<>();
    @Getter
    private final Deque<OpenedPosition> openedPositions = new LinkedList<>();
    @Getter
    private final Deque<ClosedPosition> closedPositions = new LinkedList<>();

    public Positions(Deque<Transaction> transactions, Deque<SecurityEventCashFlow> redemptions) {
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
            if (!this.openedPositions.isEmpty() || this.positionHistories.getLast().getOpenedPositions() != 0) {
                throw new RuntimeException("Предоставлены не все транзакции по бумаге " +
                        isin + ", в истории портфеля есть событие погашения номинала облигаций по " +
                        redemptions.stream().mapToInt(SecurityEventCashFlow::getCount).sum() +
                        " бумагам, однако в портфеле остались " + this.positionHistories.getLast().getOpenedPositions() +
                        " открытые позиции");
            }
        }
    }

    private void updateSecuritiesPastPositions(Queue<Transaction> transactions) {
        int openedPosition = (!this.positionHistories.isEmpty()) ? this.positionHistories.peekLast().getOpenedPositions() : 0;
        for (Transaction transaction : transactions) {
            openedPosition += transaction.getCount();
            this.positionHistories.add(new PositionHistory(transaction, openedPosition));
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

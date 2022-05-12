/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.report;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.util.Assert;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

import static java.lang.Integer.min;
import static java.lang.Integer.signum;
import static java.lang.Math.abs;

@Getter
@Slf4j
public class FifoPositions {

    private final Deque<Transaction> transactions;
    private final Deque<SecurityEventCashFlow> redemptions;
    private final Deque<PositionHistory> positionHistories = new LinkedList<>();
    private final Deque<OpenedPosition> openedPositions = new LinkedList<>();
    private final Deque<ClosedPosition> closedPositions = new LinkedList<>();
    private final int currentOpenedPositionsCount;

    public FifoPositions(Deque<Transaction> transactions, Deque<SecurityEventCashFlow> redemptions) {
        this.transactions = transactions;
        this.redemptions = redemptions;
        updateSecuritiesPastPositions(transactions);
        processTransactions(transactions);
        processRedemptions(redemptions);
        this.currentOpenedPositionsCount = Optional.ofNullable(positionHistories.peekLast())
                .map(PositionHistory::getOpenedPositions)
                .orElse(0);
    }

    private void processTransactions(Deque<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            if (isIncreasePosition(transaction)) {
                this.openedPositions.add(new OpenedPosition(transaction));
            } else {
                closePositions(transaction, CashFlowType.PRICE);
            }
        }
    }

    private void processRedemptions(Deque<SecurityEventCashFlow> redemptions) {
        if (!redemptions.isEmpty() && (redemptions.peek() != null)) {
            int security = redemptions.peek().getSecurity();
            LinkedList<Transaction> redemptionTransactions = redemptions.stream()
                    .map(FifoPositions::convertBondRedemptionToTransaction)
                    .collect(Collectors.toCollection(LinkedList::new));
            updateSecuritiesPastPositions(redemptionTransactions);
            redemptionTransactions.forEach(
                    redemption -> closePositions(redemption, CashFlowType.REDEMPTION));
            if (!this.openedPositions.isEmpty() || this.positionHistories.getLast().getOpenedPositions() != 0) {
                log.error("Предоставлены не все транзакции по бумаге " +
                        security + ", в истории портфеля есть событие погашения номинала облигаций по " +
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
        return position == null ||
                position.getUnclosedPositions() == 0 ||
                (signum(transaction.getCount()) == signum(position.getUnclosedPositions()));
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
            openedPositions.add(new OpenedPosition(closing, signum(closing.getCount()) * closingCount));
        }
    }

    /**
     * Converts bonds redemption event to transaction
     */
    private static Transaction convertBondRedemptionToTransaction(SecurityEventCashFlow redemption) {
        Assert.isTrue(redemption.getEventType() == CashFlowType.REDEMPTION,
            () -> "Ожидается событие погашения номинала облигации, предоставлено событие " + redemption.getEventType());
        return Transaction.builder()
                .portfolio(redemption.getPortfolio())
                .security(redemption.getSecurity())
                .timestamp(redemption.getTimestamp())
                .count(-redemption.getCount())
                .build();
    }
}

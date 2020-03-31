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

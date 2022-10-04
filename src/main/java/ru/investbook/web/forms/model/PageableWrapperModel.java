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

package ru.investbook.web.forms.model;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@RequiredArgsConstructor
public class PageableWrapperModel<T> {
    private final Page<T> page;

    public List<T> getContent() {
        return page.getContent();
    }

    public int getTotal() {
        return page.getTotalPages();
    }

    public int getCurrent() {
        return page.getNumber();
    }

    public Integer getNext() {
        return page.hasNext() ? page.nextPageable().getPageNumber() : null;
    }

    public Integer getPrevious() {
        return page.hasPrevious() ? page.previousPageable().getPageNumber(): null;
    }

    public boolean isLast() {
        return page.isLast();
    }

    public boolean isFirst() {
        return page.isFirst();
    }
}

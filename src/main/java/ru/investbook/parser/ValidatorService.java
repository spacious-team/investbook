/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ValidatorService {
    private final Validator validator;

    public <T> void validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            violations.forEach(violation -> sb
                    .append("поле '")
                    .append(violation.getPropertyPath())
                    .append("' ")
                    .append(violation.getMessage())
                    .append("; "));
            throw new ConstraintViolationException(sb.toString(), violations);
        }
    }
}

/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.SecurityEntity;

@Transactional(readOnly = true)
public interface SecurityRepository extends JpaRepository<SecurityEntity, String> {

    @Transactional
    default void createOrUpdate(String securityId, String securityName) {
        findById(securityId).ifPresentOrElse(
                security -> security.setName(securityName),
                () -> {
                    SecurityEntity entity = new SecurityEntity();
                    entity.setId(securityId);
                    entity.setName(securityName);
                    save(entity);
                });
    }
}

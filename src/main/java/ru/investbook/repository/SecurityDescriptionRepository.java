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

package ru.investbook.repository;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.SecurityDescriptionEntity;

@Transactional(readOnly = true)
public interface SecurityDescriptionRepository extends JpaRepository<SecurityDescriptionEntity, Integer>,
        JpaSpecificationExecutor<SecurityDescriptionEntity> {

    @Transactional
    default void createOrUpdateSector(int securityId, @Nullable String sector) {
        if (existsById(securityId)) {
            updateSector(securityId, sector);
        } else {
            createSectorIfNotExists(securityId, sector);
        }
    }

    @Transactional
    @Modifying
    @Query("UPDATE SecurityDescriptionEntity SET sector = :sector WHERE security = :securityId")
    void updateSector(int securityId, @Nullable String sector);

    @Transactional
    default void createSectorIfNotExists(int securityId, @Nullable String sector) {
        if (!existsById(securityId)) {
            SecurityDescriptionEntity entity = new SecurityDescriptionEntity();
            entity.setSecurity(securityId);
            entity.setSector(sector);
            saveAndFlush(entity);
        }
    }
}

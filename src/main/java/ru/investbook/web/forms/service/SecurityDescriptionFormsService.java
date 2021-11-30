/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.web.forms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.SecurityDescriptionEntity;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.repository.SecurityDescriptionRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.web.forms.model.SecurityDescriptionModel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;


@Component
@RequiredArgsConstructor
public class SecurityDescriptionFormsService implements FormsService<SecurityDescriptionModel> {
    private final SecurityDescriptionRepository securityDescriptionRepository;
    private final SecurityRepository securityRepository;
    private final SecurityRepositoryHelper securityRepositoryHelper;

    @Transactional(readOnly = true)
    public Optional<SecurityDescriptionModel> getById(String security) {
        return securityDescriptionRepository.findById(security)
                .map(this::toModel);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SecurityDescriptionModel> getAll() {
        return securityDescriptionRepository.findAll()
                .stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void save(SecurityDescriptionModel m) {
        securityRepositoryHelper.saveAndFlushSecurity(m);
        saveAndFlushSecurityDescription(m.getSecurityId(), m.getSector());
    }

    private void saveAndFlushSecurityDescription(String securityId, String sector) {
        securityDescriptionRepository.createOrUpdateSector(securityId, sector);
        securityDescriptionRepository.flush();
    }

    private SecurityDescriptionModel toModel(SecurityDescriptionEntity e) {
        SecurityDescriptionModel m = new SecurityDescriptionModel();
        SecurityEntity security = securityRepository.findById(e.getSecurity()).orElseThrow();
        m.setSecurity(ofNullable(security.getIsin()).orElse(security.getId()),
                ofNullable(security.getName()).orElse(security.getTicker()));
        m.setSector(e.getSector());
        return m;
    }

    @Transactional
    public void delete(String securityId) {
        securityRepository.deleteById(securityId);
        securityRepository.flush();
    }
}

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

package ru.investbook.web.forms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.SecurityDescriptionEntity;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.repository.SecurityDescriptionRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.specs.SecurityDescriptionSearchSpecification;
import ru.investbook.web.forms.model.SecurityDescriptionModel;
import ru.investbook.web.forms.model.SecurityType;
import ru.investbook.web.forms.model.filter.SecurityDescriptionFormFilterModel;

import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.springframework.data.domain.Sort.Order.asc;


@Component
@RequiredArgsConstructor
public class SecurityDescriptionFormsService {
    private final SecurityDescriptionRepository securityDescriptionRepository;
    private final SecurityRepository securityRepository;
    private final SecurityRepositoryHelper securityRepositoryHelper;

    @Transactional(readOnly = true)
    public Optional<SecurityDescriptionModel> getById(Integer security) {
        return securityDescriptionRepository.findById(security)
                .map(this::toModel);
    }

    @Transactional(readOnly = true)
    public Page<SecurityDescriptionModel> getPage(SecurityDescriptionFormFilterModel filter) {
        SecurityDescriptionSearchSpecification spec = SecurityDescriptionSearchSpecification.of(
                filter.getSecurity(), filter.getSecuritySector());

        Sort sort = Sort.by(asc("sector"));
        PageRequest page = PageRequest.of(filter.getPage(), filter.getPageSize(), sort);

        return securityDescriptionRepository.findAll(spec, page)
                .map(this::toModel);
    }

    @Transactional
    public void save(SecurityDescriptionModel m) {
        int savedSecurityId = securityRepositoryHelper.saveAndFlushSecurity(m);
        securityDescriptionRepository.createOrUpdateSector(savedSecurityId, m.getSector());
    }

    private SecurityDescriptionModel toModel(SecurityDescriptionEntity e) {
        SecurityDescriptionModel m = new SecurityDescriptionModel();
        SecurityEntity security = securityRepository.findById(e.getSecurity()).orElseThrow();
        m.setSecurity(security.getId(),
                security.getIsin(),
                ofNullable(security.getName()).orElse(security.getTicker()),
                SecurityType.valueOf(security.getType()));
        m.setSector(e.getSector());
        return m;
    }

    @Transactional
    public void delete(Integer securityId) {
        securityRepository.deleteById(securityId);
        securityRepository.flush();
    }
}

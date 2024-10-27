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
import org.spacious_team.broker.pojo.SecurityQuote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.converter.SecurityQuoteConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.SecurityQuoteEntity;
import ru.investbook.entity.SecurityQuoteEntity_;
import ru.investbook.repository.SecurityQuoteRepository;
import ru.investbook.repository.specs.SecurityQuoteSearchSpecification;
import ru.investbook.web.forms.model.SecurityQuoteModel;
import ru.investbook.web.forms.model.SecurityType;
import ru.investbook.web.forms.model.filter.SecurityQuoteFormFilterModel;

import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;
import static org.springframework.util.StringUtils.hasLength;

@Service
@RequiredArgsConstructor
public class SecurityQuoteFormsService {
    private final SecurityQuoteRepository securityQuoteRepository;
    private final SecurityQuoteConverter securityQuoteConverter;
    private final SecurityRepositoryHelper securityRepositoryHelper;

    @Transactional(readOnly = true)
    public Optional<SecurityQuoteModel> getById(Integer id) {
        return securityQuoteRepository.findById(id)
                .map(this::toSecurityQuoteModel);
    }

    @Transactional(readOnly = true)
    public Page<SecurityQuoteModel> getPage(SecurityQuoteFormFilterModel filter) {
        SecurityQuoteSearchSpecification spec = SecurityQuoteSearchSpecification.of(
                filter.getSecurity(), filter.getCurrency(), filter.getDate());

        Sort sort = Sort.by(desc(SecurityQuoteEntity_.TIMESTAMP), asc("security.name"));
        PageRequest page = PageRequest.of(filter.getPage(), filter.getPageSize(), sort);

        return securityQuoteRepository.findAll(spec, page)
                .map(this::toSecurityQuoteModel);
    }

    @Transactional
    public void save(SecurityQuoteModel e) {
        int savedSecurityId = securityRepositoryHelper.saveSecurity(e);
        SecurityQuoteEntity entity = securityQuoteRepository.save(
                securityQuoteConverter.toEntity(SecurityQuote.builder()
                        .id(e.getId())
                        .security(savedSecurityId)
                        .timestamp(e.getTimestamp())
                        .quote(requireNonNull(e.getQuote()))
                        .price(e.getPrice())
                        .accruedInterest(e.getAccruedInterest())
                        .currency(hasLength(e.getCurrency()) ? e.getCurrency() : null)
                        .build()));
        e.setId(entity.getId()); // used in view
    }

    private SecurityQuoteModel toSecurityQuoteModel(SecurityQuoteEntity e) {
        SecurityQuoteModel m = new SecurityQuoteModel();
        m.setId(e.getId());
        m.setTimestamp(e.getTimestamp());
        m.setQuote(e.getQuote());
        m.setPrice(e.getPrice());
        m.setAccruedInterest(e.getAccruedInterest());
        m.setCurrency(e.getCurrency());
        SecurityEntity securityEntity = e.getSecurity();
        SecurityType securityType = e.getAccruedInterest() == null ?
            SecurityType.valueOf(securityEntity.getType()) :
            SecurityType.BOND;
        m.setSecurity(
                securityEntity.getIsin(),
                ofNullable(securityEntity.getName()).orElse(securityEntity.getTicker()),
                securityType);
        return m;
    }

    @Transactional
    public void delete(Integer id) {
        securityQuoteRepository.deleteById(id);
        securityQuoteRepository.flush();
    }
}

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
import org.spacious_team.broker.pojo.SecurityQuote;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.converter.SecurityQuoteConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.SecurityQuoteEntity;
import ru.investbook.repository.SecurityQuoteRepository;
import ru.investbook.web.forms.model.SecurityQuoteModel;
import ru.investbook.web.forms.model.SecurityType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasLength;

@Service
@RequiredArgsConstructor
public class SecurityQuoteFormsService implements FormsService<SecurityQuoteModel> {
    private final SecurityQuoteRepository securityQuoteRepository;
    private final SecurityQuoteConverter securityQuoteConverter;
    private final SecurityRepositoryHelper securityRepositoryHelper;

    @Transactional(readOnly = true)
    public Optional<SecurityQuoteModel> getById(Integer id) {
        return securityQuoteRepository.findById(id)
                .map(this::toSecurityQuoteModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityQuoteModel> getAll() {
        return securityQuoteRepository.findByOrderByTimestampDescSecurityAsc()
                .stream()
                .map(this::toSecurityQuoteModel)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void save(SecurityQuoteModel e) {
        int savedSecurityId = securityRepositoryHelper.saveAndFlushSecurity(e);
        SecurityQuoteEntity entity = securityQuoteRepository.saveAndFlush(
                securityQuoteConverter.toEntity(SecurityQuote.builder()
                        .id(e.getId())
                        .security(savedSecurityId)
                        .timestamp(e.getTimestamp())
                        .quote(e.getQuote())
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
                securityEntity.getId(),
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

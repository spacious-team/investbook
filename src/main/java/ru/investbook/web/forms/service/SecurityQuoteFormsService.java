/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.springframework.stereotype.Service;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.converter.SecurityQuoteConverter;
import ru.investbook.entity.SecurityQuoteEntity;
import ru.investbook.repository.SecurityQuoteRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.web.forms.model.SecurityQuoteModel;
import ru.investbook.web.forms.model.SecurityType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.spacious_team.broker.pojo.SecurityType.getSecurityType;

@Service
@RequiredArgsConstructor
public class SecurityQuoteFormsService implements FormsService<SecurityQuoteModel> {
    private final SecurityQuoteRepository securityQuoteRepository;
    private final SecurityRepository securityRepository;
    private final SecurityQuoteConverter securityQuoteConverter;
    private final SecurityConverter securityConverter;


    public Optional<SecurityQuoteModel> getById(Integer id) {
        return securityQuoteRepository.findById(id)
                .map(this::toSecurityQuoteModel);
    }

    @Override
    public List<SecurityQuoteModel> getAll() {
        return securityQuoteRepository.findByOrderByTimestampDescSecurityAsc()
                .stream()
                .map(this::toSecurityQuoteModel)
                .collect(Collectors.toList());
    }

    @Override
    public void save(SecurityQuoteModel e) {
        saveAndFlush(e.getSecurityId(), e.getSecurityName());
        SecurityQuoteEntity entity = securityQuoteRepository.saveAndFlush(
                securityQuoteConverter.toEntity(SecurityQuote.builder()
                        .id(e.getId())
                        .security(e.getSecurityId())
                        .timestamp(e.getTimestamp())
                        .quote(e.getQuote())
                        .price(e.getPrice())
                        .accruedInterest(e.getAccruedInterest())
                        .build()));
        e.setId(entity.getId());
    }

    private void saveAndFlush(String securityId, String securityName) {
        if (!securityRepository.existsById(securityId)) {
            securityRepository.saveAndFlush(
                    securityConverter.toEntity(Security.builder()
                            .id(securityId)
                            .name(securityName)
                            .build()));
        }
    }

    private SecurityQuoteModel toSecurityQuoteModel(SecurityQuoteEntity e) {
        SecurityQuoteModel m = new SecurityQuoteModel();
        m.setId(e.getId());
        m.setSecurity(e.getSecurity().getId(), e.getSecurity().getName());
        m.setTimestamp(e.getTimestamp());
        m.setQuote(e.getQuote());
        m.setPrice(e.getPrice());
        m.setAccruedInterest(e.getAccruedInterest());
        if (e.getAccruedInterest() == null) {
            m.setSecurityType(SecurityType.valueOf(getSecurityType(e.getSecurity().getId())));
        } else {
            m.setSecurityType(SecurityType.BOND);
        }
        return m;
    }
}

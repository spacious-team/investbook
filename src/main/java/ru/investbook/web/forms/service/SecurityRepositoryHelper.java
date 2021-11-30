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
import org.springframework.stereotype.Service;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.service.moex.MoexDerivativeCodeService;
import ru.investbook.web.forms.model.SecurityDescriptionModel;
import ru.investbook.web.forms.model.SecurityEventCashFlowModel;
import ru.investbook.web.forms.model.SecurityQuoteModel;
import ru.investbook.web.forms.model.SecurityType;
import ru.investbook.web.forms.model.TransactionModel;

import static org.spacious_team.broker.pojo.SecurityType.getSecurityType;
import static ru.investbook.web.forms.model.SecurityType.DERIVATIVE;


@Service
@RequiredArgsConstructor
public class SecurityRepositoryHelper {
    private final SecurityRepository securityRepository;
    private final MoexDerivativeCodeService moexDerivativeCodeService;

    /**
     * Generate securityId if needed, save security to DB, update securityId for model if needed
     */
    public void saveAndFlushSecurity(SecurityDescriptionModel model) {
        SecurityType securityType = SecurityType.valueOf(getSecurityType(model.getSecurityId()));
        String savedSecurityId = saveAndFlush(model.getSecurityId(), model.getSecurityName(), securityType);
        model.setSecurity(savedSecurityId, model.getSecurityName());
    }

    /**
     * Generate securityId if needed, save security to DB, update securityId for model if needed
     */
    public void saveAndFlushSecurity(SecurityEventCashFlowModel model) {
        String savedSecurityId = saveAndFlush(model.getSecurityId(), model.getSecurityName(), model.getSecurityType());
        model.setSecurity(savedSecurityId, model.getSecurityName());
    }

    /**
     * Generate securityId if needed, save security to DB, update securityId for model if needed
     */
    public void saveAndFlushSecurity(SecurityQuoteModel model) {
        String savedSecurityId = saveAndFlush(model.getSecurityId(), model.getSecurityName(), model.getSecurityType());
        model.setSecurity(savedSecurityId, model.getSecurityName());
    }

    /**
     * Generate securityId if needed, save security to DB, update securityId for model if needed
     */
    public void saveAndFlushSecurity(TransactionModel model) {
        String savedSecurityId = saveAndFlush(model.getSecurityId(), model.getSecurityName(), model.getSecurityType());
        model.setSecurity(savedSecurityId, model.getSecurityName());
    }

    /**
     * @return securityId from DB
     */
    private String saveAndFlush(String securityId, String securityName, SecurityType securityType) {
        String newSecurityId = (securityType == DERIVATIVE) ?
                moexDerivativeCodeService.convertDerivativeSecurityId(securityId) :
                securityId;
        securityRepository.createOrUpdate(newSecurityId, securityName);
        securityRepository.flush();
        return newSecurityId;
    }
}

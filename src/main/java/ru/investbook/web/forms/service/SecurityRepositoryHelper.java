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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.service.moex.MoexDerivativeCodeService;
import ru.investbook.web.forms.model.EventCashFlowModel;
import ru.investbook.web.forms.model.SecurityDescriptionModel;
import ru.investbook.web.forms.model.SecurityEventCashFlowModel;
import ru.investbook.web.forms.model.SecurityHelper;
import ru.investbook.web.forms.model.SecurityQuoteModel;
import ru.investbook.web.forms.model.SecurityType;
import ru.investbook.web.forms.model.SplitModel;
import ru.investbook.web.forms.model.TransactionModel;

import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static ru.investbook.entity.SecurityEntity.isinPattern;


@Service
@RequiredArgsConstructor
public class SecurityRepositoryHelper {
    private final SecurityRepository securityRepository;
    private final MoexDerivativeCodeService moexDerivativeCodeService;

    /**
     * Generate securityId if needed, save security to DB, update securityId for model if needed
     * @return saved security id
     */
    public int saveAndFlushSecurity(SecurityDescriptionModel m) {
        int savedSecurityId = saveAndFlush(m.getSecurityId(), m.getSecurityIsin(), m.getSecurityName(), m.getSecurityType());
        m.setSecurityId(savedSecurityId);
        return savedSecurityId;
    }

    /**
     * Generate securityId if needed, save security to DB, update securityId for model if needed
     * @return saved security id
     */
    public int saveAndFlushSecurity(EventCashFlowModel.AttachedSecurity s) {
        // EventCashFlowModel. AttachToSecurity может содержать только SHARE или BOND.
        // Тип SHARE захардкожен, тип может быть ошибочен, но для текущего алгоритма сохранения ЦБ этого типа достаточно
        return saveAndFlush(s.getSecurityIsin(), s.getSecurityName(), SecurityType.SHARE);
    }

    /**
     * Generate securityId if needed, save security to DB, update securityId for model if needed
     * @return saved security id
     */
    public int saveAndFlushSecurity(SecurityEventCashFlowModel m) {
        return saveAndFlush(m.getSecurityIsin(), m.getSecurityName(), m.getSecurityType());
    }

    /**
     * Generate securityId if needed, save security to DB, update securityId for model if needed
     * @return saved security id
     */
    public int saveAndFlushSecurity(SecurityQuoteModel m) {
        return saveAndFlush(m.getSecurityIsin(), m.getSecurityName(), m.getSecurityType());
    }

    /**
     * Generate securityId if needed, save security to DB, update securityId for model if needed
     * @return saved security id
     */
    public int saveAndFlushSecurity(TransactionModel m) {
        return saveAndFlush(m.getSecurityIsin(), m.getSecurityName(), m.getSecurityType());
    }

    /**
     * Generate securityId if needed, save security to DB, update securityId for model if needed
     * @return saved security id
     */
    public int saveAndFlushSecurity(SplitModel m) {
        return saveAndFlush(m.getSecurityIsin(), m.getSecurityName(), SecurityType.SHARE);
    }

    /**
     * @return securityId from DB
     */
    private int saveAndFlush(Integer securityId, String isin, String securityName, SecurityType securityType) {
        SecurityEntity security = ofNullable(securityId)
                .flatMap(securityRepository::findById)
                .or(() -> findSecurity(isin, securityName, securityType))
                .orElseGet(SecurityEntity::new);
        return saveAndFlush(security, isin, securityName, securityType, true);
    }

    private int saveAndFlush(String isin, String securityName, SecurityType securityType) {
        SecurityEntity security = findSecurity(isin, securityName, securityType)
                .orElseGet(SecurityEntity::new);
        return saveAndFlush(security, isin, securityName, securityType, false);
    }

    private int saveAndFlush(SecurityEntity security, String isin, String securityName,
                             SecurityType securityType, boolean forceRewriteByEmptyIsin) {
        isin = validateIsin(isin);
        if (isin == null && !forceRewriteByEmptyIsin) {
            isin = security.getIsin(); // optional isin not provided by forms, use isin from db
        }
        securityName = Objects.equals(securityName, SecurityHelper.NULL_SECURITY_NAME) ? null : securityName;
        security.setType(securityType.toDbType());
        switch (securityType) {
            case SHARE, BOND -> {
                Assert.isTrue(isin != null || securityName != null, "Отсутствует и ISIN, и наименование ЦБ");
                security.setIsin(isin);
                security.setName(securityName);
            }
            case DERIVATIVE, CURRENCY -> {
                Assert.isTrue(securityName != null, "Отсутствует тикер контракта");
                security.setTicker(moexDerivativeCodeService.convertDerivativeCode(securityName));
            }
            case ASSET -> {
                Assert.isTrue(securityName != null, "Отсутствует наименование произвольного актива");
                security.setName(securityName);
            }
        }
        security = securityRepository.saveAndFlush(security);
        return security.getId();
    }

    private Optional<SecurityEntity> findSecurity(String isin, String securityName, SecurityType securityType) {
        isin = validateIsin(isin);
        securityName = Objects.equals(securityName, SecurityHelper.NULL_SECURITY_NAME) ? null : securityName;

        return switch (securityType) {
            case SHARE, BOND -> {
                Assert.isTrue(isin != null || securityName != null, "Отсутствует и ISIN, и наименование ЦБ");
                String name = securityName;
                yield ofNullable(isin).flatMap(securityRepository::findByIsin)
                        .or(() -> ofNullable(name).flatMap(securityRepository::findByTicker))
                        .or(() -> ofNullable(name).flatMap(securityRepository::findByName));
            }
            case DERIVATIVE, CURRENCY -> {
                Assert.isTrue(securityName != null, "Отсутствует тикер контракта");
                yield securityRepository.findByTicker(securityName);
            }
            case ASSET -> {
                Assert.isTrue(securityName != null, "Отсутствует наименование произвольного актива");
                yield securityRepository.findByName(securityName);
            }
        };
    }

    @Nullable
    private String validateIsin(String isin) {
        isin = StringUtils.hasLength(isin) ? isin : null;
        if (isin != null && !isinPattern.matcher(isin).matches()) {
            throw new IllegalArgumentException("Невалидный ISIN: " + isin);
        }
        return isin;
    }
}

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

package ru.investbook.parser.investbook;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.ReportTables;
import org.spacious_team.broker.report_parser.api.ReportTablesFactory;
import org.springframework.stereotype.Component;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.parser.SecurityRegistrar;
import ru.investbook.repository.SecurityRepository;

@Component
@RequiredArgsConstructor
public class InvestbookReportTablesFactory implements ReportTablesFactory {

    private final SecurityRegistrar securityRegistrar;
    private final SecurityRepository securityRepository;
    private final SecurityConverter securityConverter;

    @Override
    public boolean canCreate(BrokerReport brokerReport) {
        return brokerReport instanceof InvestbookBrokerReport;
    }

    @Override
    public ReportTables create(BrokerReport brokerReport) {
        return new InvestbookReportTables(brokerReport, securityRegistrar, securityRepository, securityConverter);
    }
}

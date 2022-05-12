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

package ru.investbook.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.BrokerReportFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.investbook.parser.MailboxReportParserService;
import ru.investbook.web.model.MailboxDescriptor;

import java.util.Collection;

import static java.util.Collections.singleton;
import static ru.investbook.web.ReportControllerHelper.errorPage;
import static ru.investbook.web.ReportControllerHelper.getBrokerNames;

@Controller
@RequiredArgsConstructor
@RequestMapping("/broker-email-reports")
@Slf4j
public class BrokerEmailReportController {

    private final Collection<BrokerReportFactory> brokerReportFactories;
    private final MailboxReportParserService mailboxReportParserService;
    private MailboxDescriptor defaultMailboxDescriptor;

    @GetMapping
    public String getPage(Model model) {
        model.addAttribute("brokerNames", getBrokerNames(brokerReportFactories));
        model.addAttribute("mailboxDescriptor",
                defaultMailboxDescriptor == null ? new MailboxDescriptor() : defaultMailboxDescriptor);
        return "broker-email-reports";
    }

    @PostMapping
    public Object uploadBrokerReports(@ModelAttribute("mailboxDescriptor") MailboxDescriptor mailbox,
                                      ModelAndView modelAndView) {
        try {
            int parsedReportCount = mailboxReportParserService.parseReports(mailbox);
            modelAndView.addObject("message", parsedReportCount + " отчета(-ов) загружено " +
                    "из почтового ящика " + mailbox.getLogin() + " на " + mailbox.getServer());
            modelAndView.setViewName("success");
            defaultMailboxDescriptor = mailbox;
            defaultMailboxDescriptor.setPassword(null);
            return modelAndView;
        } catch (Exception e) {
            return errorPage(
                    "Возможно не удалось подключиться к почтовому серверу, проверьте параметры подключения",
                    singleton(e));
        }
    }
}

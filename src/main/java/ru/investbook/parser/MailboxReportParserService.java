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

package ru.investbook.parser;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.investbook.web.model.MailboxDescriptor;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.SearchTerm;
import java.time.Duration;
import java.util.Properties;
import java.util.stream.Stream;

import static java.lang.System.nanoTime;
import static javax.mail.Flags.Flag.SEEN;
import static org.springframework.util.StringUtils.hasLength;


@Component
@RequiredArgsConstructor
@Slf4j
public class MailboxReportParserService {

    private final Flags SEEN_FLAG = new Flags(SEEN);
    private final BrokerReportParserService brokerReportParserService;

    @SneakyThrows
    public int parseReports(MailboxDescriptor mailbox) {
        long t0 = nanoTime();
        Session session = getSession(mailbox);
        Store store = session.getStore();
        store.connect(mailbox.getServer(), mailbox.getPort(), mailbox.getLogin(), mailbox.getPassword());
        String folder = mailbox.getFolder();
        Folder inbox = hasLength(folder) ? store.getFolder(folder) : store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);
        SearchTerm searchTerm = getSearchTerm(mailbox);
        Message[] messages = inbox.search(searchTerm);
        log.info("Найдено {} не прочитанных писем, удовлетворяющих фильтру", messages.length);
        int parsedReportCount = Stream.of(messages)
                .parallel()
                .mapToInt(message -> handleMessage(message, mailbox))
                .sum();
        log.info("{} отчетов загружено с почтового ящика {} на {} за {}",
                parsedReportCount, mailbox.getLogin(), mailbox.getServer(), Duration.ofNanos(nanoTime() - t0));
        return parsedReportCount;
    }

    private Session getSession(MailboxDescriptor mailbox) {
        Properties properties = new Properties();
        properties.put("mail.debug", "false");
        properties.put("mail.store.protocol", mailbox.isEnableSsl() ? "imaps" : "imap");
        properties.put("mail.imap.connectiontimeout", 30_000); // ms
        properties.put("mail.imap.timeout", 30_000); //ms
        properties.put("mail.imap.connectionpoolsize", 4 * Runtime.getRuntime().availableProcessors());
        properties.put("mail.imap.minidletime", 10_000); // ms
        properties.put("mail.imap.separatestoreconnection", true);

        return Session.getDefaultInstance(properties);
    }

    @SneakyThrows
    private SearchTerm getSearchTerm(MailboxDescriptor mailbox) {
        SearchTerm notSeen = new FlagTerm(SEEN_FLAG, false);
        String filterByFrom = mailbox.getFilterByFrom();
        if (hasLength(filterByFrom)) {
            return new AndTerm(new SearchTerm[]{
                    notSeen,
                    new FromTerm(new InternetAddress(filterByFrom))});
        } else {
            return notSeen;
        }
    }

    /**
     * @return number of successfully parsed reports
     */
    private int handleMessage(Message message, MailboxDescriptor mailbox) {
        int reportCnt = 0;
        boolean isSeen = true;
        try {
            isSeen = message.getFlags().contains(SEEN);
            Object content = message.getContent(); // sets SEEN to true
            if (content instanceof Multipart) {
                Multipart multipart = (Multipart) message.getContent();
                for (int i = 0, cnt = multipart.getCount(); i < cnt; i++) {
                    if (handleBodyPart(multipart.getBodyPart(i), mailbox)) {
                        reportCnt++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Mail parsing error", e);
        } finally {
            try {
                if (reportCnt == 0 && !isSeen) {
                    message.setFlag(SEEN, false);
                }
            } catch (Exception ignore) {
            }
        }
        return reportCnt;
    }

    /**
     * @return true if body contains report and it was successfully parsed
     */
    private boolean handleBodyPart(BodyPart bodyPart, MailboxDescriptor mailbox) {
        try {
            if (bodyPart.getFileName() != null) {
                DataHandler dataHandler = bodyPart.getDataHandler();
                DataSource dataSource = dataHandler.getDataSource();
                brokerReportParserService.parseReport(dataSource.getInputStream(), dataSource.getName(), mailbox.getBroker());
                return true;
            }
        } catch (Exception e) {
            log.error("Вложенный файл не является отчетом брокера", e);
        }
        return false;
    }
}

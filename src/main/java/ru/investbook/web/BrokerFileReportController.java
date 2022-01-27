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

package ru.investbook.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.BrokerReportFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.investbook.parser.BrokerReportParserService;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.springframework.util.StringUtils.hasLength;
import static ru.investbook.web.ReportControllerHelper.errorPage;
import static ru.investbook.web.ReportControllerHelper.getBrokerNames;

@Controller
@RequiredArgsConstructor
@RequestMapping("/broker-file-reports")
@Slf4j
public class BrokerFileReportController {

    private final List<BrokerReportFactory> brokerReportFactories;
    private final BrokerReportParserService brokerReportParserService;


    @GetMapping
    public String getPage(Model model) {
        model.addAttribute("brokerNames", getBrokerNames(brokerReportFactories));
        return "broker-file-reports";
    }

    @PostMapping
    public ResponseEntity<String> uploadBrokerReports(@RequestParam("reports") MultipartFile[] reports,
                                                      @RequestParam(name = "broker", required = false) String broker) {
        Collection<Exception> exceptions = new ConcurrentLinkedQueue<>();
        Arrays.stream(reports)
                .parallel()
                .filter(Objects::nonNull)
                .filter(report -> !report.isEmpty())
                .forEach(report -> {
                    uploadReport(report, broker, exceptions);
                });
        if (exceptions.isEmpty()) {
            return ok();
        } else {
            return errorPage(hasLength(broker) ? "" : "Попробуйте повторить загрузку, указав Брокера", exceptions);
        }
    }

    private void uploadReport(MultipartFile report, String broker, Collection<Exception> exceptions) {
        try (InputStream inputStream = report.getInputStream()) { // creates new input stream
            brokerReportParserService.parseReport(inputStream, report.getOriginalFilename(), broker);
        } catch (Exception e) {
            exceptions.add(e);
        }
    }

    private ResponseEntity<String> ok() {
        return ResponseEntity.ok("""
                Отчеты загружены <a href="/">[ok]</a>
                <script type="text/javascript">document.location.href="/"</script>
                """);
    }
}
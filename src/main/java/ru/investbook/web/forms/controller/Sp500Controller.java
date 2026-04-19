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

package ru.investbook.web.forms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.investbook.entity.StockMarketIndexEntity;
import ru.investbook.repository.StockMarketIndexRepository;
import ru.investbook.service.Sp500Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/sp500")
@RequiredArgsConstructor
public class Sp500Controller {
    private static final String RETURN_URL = "upload-excel-file";
    private final Sp500Service sp500Service;
    private final StockMarketIndexRepository stockMarketIndexRepository;

    @GetMapping("update")
    public String updateSp500(Model model) {
        // Link has been copied from https://www.spglobal.com -> menu -> indices -> S&P 500 -> 10 Years
        model.addAttribute("title", "S&P 500");
        model.addAttribute("requestURL", "http://www.spglobal.com/spdji/en/idsexport/file.xls?" +
                "hostIdentifier=" + UUID.randomUUID() +
                "&redesignExport=true" +
                "&languageId=1" +
                "&selectedModule=PerformanceGraphView" +
                "&selectedSubModule=Graph" +
                "&yearFlag=tenYearFlag" +
                "&indexId=340");
        model.addAttribute("returnURL", "/sp500/" + RETURN_URL);
        return "get-by-browser";
    }

    @PostMapping(RETURN_URL)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile sp500ExcelFile) throws IOException {
        InputStream is = sp500ExcelFile.getInputStream();
        String result = updateSp500Index(is);
        return ResponseEntity.ok(Map.of(
                "title", "S&P 500",
                "message", result));
    }

    public String updateSp500Index(InputStream sp500ExcelFile) {
        sp500Service.update(sp500ExcelFile);
        return stockMarketIndexRepository.findFirstBySp500NotNullOrderByDateDesc()
                .map(StockMarketIndexEntity::getDate)
                .map(date -> "S&P 500 обновлен по " + date + " включительно")
                .orElse("Запрос выполнен, но сервер https://www.spglobal.com/ не вернул данные");
    }
}

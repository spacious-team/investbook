/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.portfolio.portfolio.view;

import com.google.common.jimfs.Jimfs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.portfolio.portfolio.view.excel.ExcelView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PortfolioViewRestController {
    private final ExcelView excelView;
    private FileSystem jimfs = Jimfs.newFileSystem();

    @GetMapping("/portfolio")
    public void getExelView(HttpServletResponse response) throws IOException {
        long t0 = System.nanoTime();
        String fileName = "portfolio.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-disposition", "attachment; filename=" + fileName);
        Path path = jimfs.getPath(fileName);
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            excelView.writeTo(book);
            book.write(Files.newOutputStream(path));
        }
        IOUtils.copy(Files.newInputStream(path), response.getOutputStream());
        response.flushBuffer();
        log.info("Отчет {} сформирован за {}", path.getFileName(), Duration.ofNanos(System.nanoTime() - t0));
    }

}

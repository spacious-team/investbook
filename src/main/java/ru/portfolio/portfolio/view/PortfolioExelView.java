package ru.portfolio.portfolio.view;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.repository.PortfolioRepository;
import ru.portfolio.portfolio.repository.SecurityRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PortfolioExelView {
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;

    private static XSSFCellStyle getHeaderStyle(XSSFWorkbook book) {
        XSSFCellStyle style = getDefalutStyle(book);
        style.getFont().setBold(true);
        return style;
    }

    private static XSSFCellStyle getSecurityColumn(XSSFWorkbook book) {
        XSSFCellStyle style = getDefalutStyle(book);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private static XSSFCellStyle getDefalutStyle(XSSFWorkbook book) {
        XSSFFont font = book.createFont();
        XSSFCellStyle style = book.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    public void writeTo(Path path) throws IOException {
        XSSFWorkbook book = new XSSFWorkbook();
        XSSFCellStyle style = getDefalutStyle(book);
        XSSFCellStyle securityStyle = getSecurityColumn(book);
        XSSFCellStyle headerStyle = getHeaderStyle(book);
        for (PortfolioEntity entity : portfolioRepository.findAll()) {
            XSSFSheet sheet = book.createSheet(entity.getPortfolio());
            writeHeader(sheet, headerStyle);
            int col = 1;
            for (String isin : transactionRepository.findDistinctIsinByPortfolio(entity)) {
                Optional<SecurityEntity> security = securityRepository.findByIsin(isin);
                if (security.isPresent()) {
                    XSSFRow row = sheet.createRow(col++);
                    XSSFCell cell = row.createCell(0);
                    cell.setCellValue(security.get().getName());
                    cell.setCellStyle(securityStyle);
                }
            }
        }
        book.write(Files.newOutputStream(path));
        book.close();
    }

    private void writeHeader(XSSFSheet sheet, XSSFCellStyle style) {
        XSSFRow row = sheet.createRow(0);
        row.setHeight((short)-1);
        String[] header = new String[] {
                "Бумага", "Дата покупки", "Количество", "Стоимость, % номинала/руб", "Стоимость (без НКД и комиссии)",
                "НКД уплоченный", "Комиссия покупки", "Дата продажи", "Стоимость прод/погаш", "НКД при продаже",
                "Выплачены купоны (после налога)", "Амортизация облигации", "Дивиденды (после налога)",
                "Комиссия продажи/погашения", "Налог (с разницы курсов)", "Доходность годовых, %" };
        for (int i = 0; i < header.length; i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(header[i]);
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, 14 * 256);
        }
        sheet.setColumnWidth(0, 45 * 256);
    }
}

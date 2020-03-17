package ru.portfolio.portfolio.view;

import com.google.common.jimfs.Jimfs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.portfolio.portfolio.view.excel.StockMarketProfitExcelView;

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
    private final StockMarketProfitExcelView stockMarketProfitExcelView;
    private FileSystem jimfs = Jimfs.newFileSystem();

    @GetMapping("/portfolio")
    public void getExelView(HttpServletResponse response) throws IOException {
        long t0 = System.nanoTime();
        String fileName = "portfolio.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-disposition", "attachment; filename=" + fileName);
        Path path = jimfs.getPath(fileName);
        stockMarketProfitExcelView.writeTo(path);
        IOUtils.copy(Files.newInputStream(path), response.getOutputStream());
        response.flushBuffer();
        log.info("Отчет {} сформирован за {}", path.getFileName(), Duration.ofNanos(System.nanoTime() - t0));
    }

}

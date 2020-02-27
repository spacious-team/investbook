package ru.portfolio.portfolio.view;

import com.google.common.jimfs.Jimfs;
import lombok.RequiredArgsConstructor;
import org.apache.poi.util.IOUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequiredArgsConstructor
public class PortfolioViewRestController {
    private final PortfolioExelView portfolioExelView;
    private FileSystem jimfs = Jimfs.newFileSystem();

    @GetMapping("/portfolio")
    public void getExelView(HttpServletResponse response) throws IOException {
        String fileName = "portfolio.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-disposition", "attachment; filename=" + fileName);
        Path path = jimfs.getPath(fileName);
        portfolioExelView.writeTo(path);
        IOUtils.copy(Files.newInputStream(path), response.getOutputStream());
        response.flushBuffer();
    }

}

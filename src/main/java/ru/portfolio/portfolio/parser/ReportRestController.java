package ru.portfolio.portfolio.parser;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.portfolio.portfolio.parser.psb.PsbReportParserService;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ReportRestController {
    private final PsbReportParserService psbReportParserService;
    private FileSystem jimfs = Jimfs.newFileSystem(Configuration.unix());

    @PostMapping("/a")
    public String a() {
        throw new IllegalArgumentException();
    }

    @PostMapping("/reports")
    public String post(@RequestParam("reports") MultipartFile[] reports,
                                                @RequestParam(name = "format", required = false) String format) {
       if (format == null || format.isEmpty()) {
            format = "psb";
        }
        format = format.toLowerCase();
        List<Exception> exceptions = new ArrayList<>();
        for (MultipartFile report : reports) {
            try {
                if (report == null || report.isEmpty()) {
                    continue;
                }
                byte[] bytes = report.getBytes();
                String originalFilename = report.getOriginalFilename();
                Path path = jimfs.getPath(originalFilename != null ? originalFilename : UUID.randomUUID().toString());
                Files.write(path, bytes);
                if ("psb".equals(format)) {
                    psbReportParserService.parse(path);
                } else {
                    throw new IllegalArgumentException("Неизвестный формат " + format);
                }
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (exceptions.isEmpty()) {
            return "ok";
        } else {
            throw new RuntimeException(exceptions.stream().map(Throwable::getMessage).collect(Collectors.joining(", ")));
        }
    }
}

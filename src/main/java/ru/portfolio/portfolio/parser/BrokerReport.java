package ru.portfolio.portfolio.parser;

import org.apache.poi.ss.usermodel.Sheet;

import java.nio.file.Path;
import java.time.Instant;

public interface BrokerReport extends AutoCloseable {
    Sheet getSheet();
    String getPortfolio();
    Path getPath();
    Instant convertToInstant(String value);
}

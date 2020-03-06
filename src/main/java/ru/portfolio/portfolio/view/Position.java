package ru.portfolio.portfolio.view;

import java.time.Instant;

public interface Position {
    boolean wasOpenedAtTheInstant(Instant pastInstant);
    int getCount();
}

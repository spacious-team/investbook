package ru.portfolio.portfolio.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends IllegalArgumentException {
    public BadRequestException(String s) {
        super(s);
    }
}

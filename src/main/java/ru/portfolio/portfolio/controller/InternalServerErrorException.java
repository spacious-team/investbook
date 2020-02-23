package ru.portfolio.portfolio.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerErrorException extends IllegalArgumentException {
    public InternalServerErrorException(String s, Throwable t) {
        super(s, t);
    }
}

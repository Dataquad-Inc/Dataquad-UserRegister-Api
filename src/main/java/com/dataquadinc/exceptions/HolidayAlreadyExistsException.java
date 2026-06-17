package com.dataquadinc.exceptions;

public class HolidayAlreadyExistsException extends RuntimeException {
    public HolidayAlreadyExistsException(String message) {
        super(message);
    }
}
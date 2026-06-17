package com.dataquadinc.exceptions;

public class HolidayNotFoundException extends RuntimeException {
    public HolidayNotFoundException(String message) {
        super(message);
    }
}
package com.dataquadinc.exceptions;

public class InvalidHolidayTypeException extends RuntimeException {
    public InvalidHolidayTypeException(String message) {
        super(message);
    }
}
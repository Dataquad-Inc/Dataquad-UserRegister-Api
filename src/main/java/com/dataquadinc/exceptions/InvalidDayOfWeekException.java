package com.dataquadinc.exceptions;

public class InvalidDayOfWeekException extends RuntimeException {
    public InvalidDayOfWeekException(String message) {
        super(message);
    }
}
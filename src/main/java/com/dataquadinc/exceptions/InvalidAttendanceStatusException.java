package com.dataquadinc.exceptions;

public class InvalidAttendanceStatusException extends RuntimeException {
    public InvalidAttendanceStatusException(String message) {
        super(message);
    }
}
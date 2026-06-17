package com.dataquadinc.exceptions;

public class PastDateAttendanceException extends RuntimeException {
    public PastDateAttendanceException(String message) {
        super(message);
    }
}
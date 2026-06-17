package com.dataquadinc.exceptions;

public class AttendanceRecordNotFoundException extends RuntimeException {
    public AttendanceRecordNotFoundException(String message) {
        super(message);
    }
}
package com.dataquadinc.exceptions;

public class LeaveQuotaExceededException extends AttendanceException {
    public LeaveQuotaExceededException(String message) {
        super(message);
    }
}
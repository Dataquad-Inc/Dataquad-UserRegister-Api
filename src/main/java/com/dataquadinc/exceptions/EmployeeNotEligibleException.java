package com.dataquadinc.exceptions;

public class EmployeeNotEligibleException extends RuntimeException {
    public EmployeeNotEligibleException(String message) {
        super(message);
    }
}
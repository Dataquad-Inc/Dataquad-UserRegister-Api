package com.dataquadinc.exceptions;

public class SummaryNotFoundException extends RuntimeException {
    public SummaryNotFoundException(String message) {
        super(message);
    }
}
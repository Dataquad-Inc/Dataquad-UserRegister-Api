package com.dataquadinc.exceptions;

public class CycleNotFoundException extends RuntimeException {
    public CycleNotFoundException(String message) {
        super(message);
    }
}
package com.dataquadinc.exceptions;

public class CycleClosedException extends RuntimeException {
    public CycleClosedException(String message) {
        super(message);
    }
}
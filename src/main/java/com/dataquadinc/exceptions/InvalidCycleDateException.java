package com.dataquadinc.exceptions;

public class InvalidCycleDateException extends RuntimeException {
    public InvalidCycleDateException(String message) {
        super(message);
    }
}
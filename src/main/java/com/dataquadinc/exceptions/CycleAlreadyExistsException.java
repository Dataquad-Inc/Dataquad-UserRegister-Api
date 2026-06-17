package com.dataquadinc.exceptions;

public class CycleAlreadyExistsException extends RuntimeException {
    public CycleAlreadyExistsException(String message) {
        super(message);
    }
}
package com.taskmanagement.exception;

/** Thrown for malformed or semantically invalid requests that aren't covered by bean validation. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}

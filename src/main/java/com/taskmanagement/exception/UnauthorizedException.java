package com.taskmanagement.exception;

/** Thrown for authentication failures (invalid credentials, invalid/expired token, ...). */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}

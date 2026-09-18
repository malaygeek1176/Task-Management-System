package com.taskmanagement.exception;

/** Thrown when an operation would violate a uniqueness constraint (e.g. duplicate email). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

package com.university.ums.exception;

/** Mapped to 400 Bad Request for rule violations that annotations cannot express. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}

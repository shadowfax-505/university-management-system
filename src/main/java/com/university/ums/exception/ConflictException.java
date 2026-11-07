package com.university.ums.exception;

/** Mapped to 409 Conflict: duplicate username/email/studentId, or deleting the last admin. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

package com.university.ums.exception;

/** Mapped to 404 Not Found by GlobalExceptionHandler. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " with id " + id + " not found");
    }
}

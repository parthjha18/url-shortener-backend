package com.parth.urlshortener.exception;

/**
 * Thrown when the submitted URL fails validation.
 * Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(String message) {
        super(message);
    }
}

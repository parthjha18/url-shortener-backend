package com.parth.urlshortener.exception;

/**
 * Thrown when a requested short code does not exist in the database.
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class UrlNotFoundException extends RuntimeException {

    public UrlNotFoundException(String shortCode) {
        super("No URL found for short code: " + shortCode);
    }
}

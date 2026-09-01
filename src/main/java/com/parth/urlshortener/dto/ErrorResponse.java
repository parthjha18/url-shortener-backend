package com.parth.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic error response envelope returned by the
 * {@link com.parth.urlshortener.exception.GlobalExceptionHandler}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** HTTP status code (e.g. 400, 404, 500) */
    private int status;

    /** Human-readable error description */
    private String message;

    /** Timestamp of the error occurrence */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}

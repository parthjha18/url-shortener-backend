package com.parth.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned by the POST /shorten endpoint.
 * Contains only the fully-qualified short URL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortenResponse {

    /** e.g. "http://localhost:8080/aB3dF2x" */
    private String shortUrl;
}

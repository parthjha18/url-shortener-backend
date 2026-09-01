package com.parth.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for the GET /stats/{shortCode} analytics endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlStatsResponse {

    private String originalUrl;

    private String shortCode;

    private Long clickCount;

    /** Formatted as ISO date for readability, e.g. "2026-03-09" */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;
}

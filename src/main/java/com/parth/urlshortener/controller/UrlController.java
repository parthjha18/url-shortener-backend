package com.parth.urlshortener.controller;

import com.parth.urlshortener.dto.ShortenRequest;
import com.parth.urlshortener.dto.ShortenResponse;
import com.parth.urlshortener.dto.UrlStatsResponse;
import com.parth.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the three core URL Shortener APIs.
 *
 * Endpoints:
 *   POST  /shorten        — create a short URL
 *   GET   /{shortCode}    — redirect (HTTP 302) to the original URL
 *   GET   /stats/{code}   — return click analytics for a short URL
 */
@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    // ------------------------------------------------------------------ //
    //  POST /shorten  — accepts a long URL, returns the short URL
    // ------------------------------------------------------------------ //

    /**
     * Shorten a URL.
     *
     * @param request validated DTO containing the original URL
     * @return 201 Created with the short URL in the response body
     */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortenResponse response = urlService.shortenUrl(request.getUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ------------------------------------------------------------------ //
    //  GET /{shortCode}  — 302 redirect to the original URL
    // ------------------------------------------------------------------ //

    /**
     * Redirect the caller to the original URL mapped to the given short code.
     * The click counter is atomically incremented.
     *
     * A 302 (Found) status is used instead of 301 (Moved Permanently) so
     * browsers do not permanently cache the redirect and we can continue
     * tracking clicks accurately.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = urlService.resolveAndTrackClick(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, originalUrl);

        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    // ------------------------------------------------------------------ //
    //  GET /stats/{shortCode}  — click analytics
    // ------------------------------------------------------------------ //

    /**
     * Return analytics (click count, creation date, etc.) for a short code.
     */
    @GetMapping("/stats/{shortCode}")
    public ResponseEntity<UrlStatsResponse> stats(@PathVariable String shortCode) {
        UrlStatsResponse stats = urlService.getStats(shortCode);
        return ResponseEntity.ok(stats);
    }
}

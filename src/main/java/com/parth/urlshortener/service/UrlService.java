package com.parth.urlshortener.service;

import com.parth.urlshortener.dto.ShortenResponse;
import com.parth.urlshortener.dto.UrlStatsResponse;
import com.parth.urlshortener.exception.UrlNotFoundException;
import com.parth.urlshortener.model.UrlEntity;
import com.parth.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core business-logic layer for the URL Shortener.
 *
 * Caching: {@code resolveUrl} results are cached in Redis under "urls::{shortCode}".
 * Click counts are incremented atomically in the DB and are always read fresh —
 * no cache eviction on redirect.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ------------------------------------------------------------------ //
    //  1.  Shorten a long URL
    // ------------------------------------------------------------------ //

    /**
     * Accept a long URL, generate a unique short code, persist the mapping,
     * and return the fully-qualified short URL.
     */
    @Transactional
    public ShortenResponse shortenUrl(String originalUrl) {
        String shortCode = shortCodeGenerator.generate();

        UrlEntity entity = UrlEntity.builder()
                .originalUrl(originalUrl)
                .shortCode(shortCode)
                .clickCount(0L)
                .build();

        urlRepository.save(entity);

        log.info("Shortened URL: {} → {}", originalUrl, shortCode);

        String shortUrl = baseUrl + "/" + shortCode;
        return new ShortenResponse(shortUrl);
    }

    // ------------------------------------------------------------------ //
    //  2.  Resolve short code → original URL  (cached)
    // ------------------------------------------------------------------ //

    /**
     * Look up the original URL for a given short code.
     * The result is cached in Redis (if available) to avoid repeated DB hits.
     */
    @Cacheable(value = "urls", key = "#shortCode")
    @Transactional(readOnly = true)
    public String resolveUrl(String shortCode) {
        UrlEntity entity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        return entity.getOriginalUrl();
    }

    // ------------------------------------------------------------------ //
    //  3.  Redirect — resolve + increment click count
    // ------------------------------------------------------------------ //

    /**
     * Resolve the original URL and atomically increment the click counter.
     *
     * The increment is a single UPDATE statement (no read-modify-write) so
     * concurrent redirects cannot lose counts to a race condition.
     * No cache eviction is needed because the "urls" cache stores only the
     * URL string — click counts are always read directly from the database.
     */
    @Transactional
    public String resolveAndTrackClick(String shortCode) {
        UrlEntity entity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        urlRepository.incrementClickCount(shortCode);

        log.info("Redirecting {} → {}", shortCode, entity.getOriginalUrl());
        return entity.getOriginalUrl();
    }

    // ------------------------------------------------------------------ //
    //  4.  Analytics — click stats for a short code
    // ------------------------------------------------------------------ //

    /**
     * Return analytics data for a given short code.
     */
    @Transactional(readOnly = true)
    public UrlStatsResponse getStats(String shortCode) {
        UrlEntity entity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        return UrlStatsResponse.builder()
                .originalUrl(entity.getOriginalUrl())
                .shortCode(entity.getShortCode())
                .clickCount(entity.getClickCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

package com.parth.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the URL Shortener application.
 *
 * @EnableCaching activates Spring's caching abstraction so that
 * Redis-backed @Cacheable / @CacheEvict annotations in the service
 * layer are honoured at runtime.
 */
@SpringBootApplication
@EnableCaching
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}

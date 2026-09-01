package com.parth.urlshortener.service;

import com.parth.urlshortener.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates unique, URL-safe short codes using Base62 encoding.
 *
 * The character set (a-z, A-Z, 0-9) yields 62^7 ≈ 3.5 trillion
 * possible codes, which is more than sufficient for a production
 * URL shortener at scale.
 *
 * Uniqueness is guaranteed by checking the database before returning
 * a code. In the astronomically unlikely event of a collision the
 * generator retries with a new random code.
 */
@Component
public class ShortCodeGenerator {

    private static final String BASE62_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int MAX_RETRIES = 10;

    private final SecureRandom random = new SecureRandom();
    private final UrlRepository urlRepository;
    private final int codeLength;

    public ShortCodeGenerator(
            UrlRepository urlRepository,
            @Value("${app.short-code.length:7}") int codeLength) {
        this.urlRepository = urlRepository;
        this.codeLength = codeLength;
    }

    /**
     * Generate a unique short code that does not yet exist in the database.
     *
     * @return a unique Base62 short code string
     * @throws IllegalStateException if a unique code cannot be generated
     *         within {@link #MAX_RETRIES} attempts
     */
    public String generate() {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            String code = randomBase62(codeLength);
            if (!urlRepository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException(
                "Failed to generate a unique short code after " + MAX_RETRIES + " attempts");
    }

    /**
     * Build a random string of the given length from the Base62 alphabet.
     */
    private String randomBase62(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_CHARS.charAt(random.nextInt(BASE62_CHARS.length())));
        }
        return sb.toString();
    }
}

package com.parth.urlshortener.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA entity mapped to the "urls" table.
 *
 * Implements {@link Serializable} so instances can be stored in Redis cache.
 * Lombok generates getters, setters, toString, equals/hashCode, and
 * builder methods to keep the class concise.
 */
@Entity
@Table(name = "urls", indexes = {
        // Index on short_code for O(1) lookups during redirection
        @Index(name = "idx_short_code", columnList = "short_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UrlEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Auto-incremented primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The original (long) URL submitted by the user */
    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    /** The generated short code (e.g. "aB3dF2x") — unique across the table */
    @Column(name = "short_code", nullable = false, unique = true, length = 10)
    private String shortCode;

    /** Number of times the short link has been opened */
    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private Long clickCount = 0L;

    /** Timestamp set automatically when the row is first inserted */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

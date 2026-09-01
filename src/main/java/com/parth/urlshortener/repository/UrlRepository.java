package com.parth.urlshortener.repository;

import com.parth.urlshortener.model.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UrlEntity}.
 *
 * Spring auto-generates the implementation at runtime —
 * we only need to declare the query-method signatures.
 */
@Repository
public interface UrlRepository extends JpaRepository<UrlEntity, Long> {

    Optional<UrlEntity> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    /**
     * Atomically increment click_count in the database.
     * Using a single UPDATE avoids the read-modify-write race condition that
     * occurs when two concurrent redirects both read count N and both write N+1.
     */
    @Modifying
    @Query("UPDATE UrlEntity u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    int incrementClickCount(@Param("shortCode") String shortCode);
}

package com.parth.urlshortener.service;

import com.parth.urlshortener.dto.ShortenResponse;
import com.parth.urlshortener.dto.UrlStatsResponse;
import com.parth.urlshortener.exception.UrlNotFoundException;
import com.parth.urlshortener.model.UrlEntity;
import com.parth.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    UrlRepository urlRepository;

    @Mock
    ShortCodeGenerator shortCodeGenerator;

    @InjectMocks
    UrlService urlService;

    // ------------------------------------------------------------------
    // shortenUrl
    // ------------------------------------------------------------------

    @Test
    void shortenUrl_persistsEntityAndReturnsShortUrl() {
        when(shortCodeGenerator.generate()).thenReturn("abc1234");
        when(urlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShortenResponse response = urlService.shortenUrl("https://example.com");

        assertThat(response.getShortUrl()).endsWith("/abc1234");
        verify(urlRepository).save(argThat(e ->
                "abc1234".equals(e.getShortCode()) &&
                "https://example.com".equals(e.getOriginalUrl()) &&
                e.getClickCount() == 0L
        ));
    }

    // ------------------------------------------------------------------
    // resolveUrl
    // ------------------------------------------------------------------

    @Test
    void resolveUrl_returnsOriginalUrlForKnownCode() {
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(entityWith("abc1234", 0L)));

        String result = urlService.resolveUrl("abc1234");

        assertThat(result).isEqualTo("https://example.com");
    }

    @Test
    void resolveUrl_throwsUrlNotFoundExceptionForUnknownCode() {
        when(urlRepository.findByShortCode("zzz")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.resolveUrl("zzz"))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("zzz");
    }

    // ------------------------------------------------------------------
    // resolveAndTrackClick
    // ------------------------------------------------------------------

    @Test
    void resolveAndTrackClick_returnsOriginalUrlAndIncrements() {
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(entityWith("abc1234", 5L)));

        String url = urlService.resolveAndTrackClick("abc1234");

        assertThat(url).isEqualTo("https://example.com");
        // Atomic DB increment — no read-modify-write
        verify(urlRepository).incrementClickCount("abc1234");
    }

    @Test
    void resolveAndTrackClick_throwsForUnknownCode() {
        when(urlRepository.findByShortCode("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.resolveAndTrackClick("bad"))
                .isInstanceOf(UrlNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // getStats
    // ------------------------------------------------------------------

    @Test
    void getStats_returnsClickCountAndOriginalUrl() {
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(entityWith("abc1234", 42L)));

        UrlStatsResponse stats = urlService.getStats("abc1234");

        assertThat(stats.getClickCount()).isEqualTo(42L);
        assertThat(stats.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(stats.getShortCode()).isEqualTo("abc1234");
    }

    @Test
    void getStats_throwsForUnknownCode() {
        when(urlRepository.findByShortCode("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.getStats("nope"))
                .isInstanceOf(UrlNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private UrlEntity entityWith(String shortCode, long clicks) {
        return UrlEntity.builder()
                .shortCode(shortCode)
                .originalUrl("https://example.com")
                .clickCount(clicks)
                .createdAt(LocalDateTime.now())
                .build();
    }
}

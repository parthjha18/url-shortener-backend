package com.parth.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parth.urlshortener.dto.ShortenRequest;
import com.parth.urlshortener.dto.ShortenResponse;
import com.parth.urlshortener.dto.UrlStatsResponse;
import com.parth.urlshortener.exception.UrlNotFoundException;
import com.parth.urlshortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UrlService urlService;

    // RateLimitFilter is a @Component in the web layer and needs StringRedisTemplate;
    // mocking it here satisfies the dependency. The filter itself fails open when
    // the mock returns null, so no request is blocked during tests.
    @MockBean
    StringRedisTemplate stringRedisTemplate;

    // ------------------------------------------------------------------
    // POST /shorten
    // ------------------------------------------------------------------

    @Test
    void postShorten_returns201WithShortUrl() throws Exception {
        when(urlService.shortenUrl("https://example.com"))
                .thenReturn(new ShortenResponse("http://localhost:8080/abc1234"));

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequest("https://example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc1234"));
    }

    @Test
    void postShorten_returns400ForBlankUrl() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postShorten_returns400ForInvalidUrl() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postShorten_returns400ForMissingBody() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // GET /{shortCode}  — redirect
    // ------------------------------------------------------------------

    @Test
    void getRedirect_returns302ToOriginalUrl() throws Exception {
        when(urlService.resolveAndTrackClick("abc1234")).thenReturn("https://example.com");

        mockMvc.perform(get("/abc1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void getRedirect_returns404ForUnknownCode() throws Exception {
        when(urlService.resolveAndTrackClick("zzz")).thenThrow(new UrlNotFoundException("zzz"));

        mockMvc.perform(get("/zzz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ------------------------------------------------------------------
    // GET /stats/{shortCode}
    // ------------------------------------------------------------------

    @Test
    void getStats_returns200WithClickCount() throws Exception {
        UrlStatsResponse stats = UrlStatsResponse.builder()
                .originalUrl("https://example.com")
                .shortCode("abc1234")
                .clickCount(17L)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
        when(urlService.getStats("abc1234")).thenReturn(stats);

        mockMvc.perform(get("/stats/abc1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(17))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"))
                .andExpect(jsonPath("$.shortCode").value("abc1234"));
    }

    @Test
    void getStats_returns404ForUnknownCode() throws Exception {
        when(urlService.getStats("zzz")).thenThrow(new UrlNotFoundException("zzz"));

        mockMvc.perform(get("/stats/zzz"))
                .andExpect(status().isNotFound());
    }
}

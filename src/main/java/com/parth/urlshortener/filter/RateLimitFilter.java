package com.parth.urlshortener.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Per-IP rate limiter using a Redis fixed-window counter.
 *
 * Algorithm:
 *   key  = "rl:{ip}:{minute-bucket}"   (e.g. "rl:1.2.3.4:28641792")
 *   INCR key  →  atomic; returns new count
 *   If count == 1, set TTL = 2 min (prevents orphaned keys after the window rolls over)
 *   If count > limit, respond 429 with Retry-After header
 *
 * Redis INCR is atomic, so concurrent requests from the same IP never race.
 * If Redis is unavailable the filter fails open — the service keeps running
 * without rate limiting rather than becoming unavailable.
 *
 * X-Forwarded-For is respected so the real client IP is used behind a
 * reverse proxy or load balancer.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final int requestsPerMinute;

    public RateLimitFilter(
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limit.requests-per-minute:60}") int requestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = getClientIp(request);

        if (isRateLimited(ip)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests — limit is "
                    + requestsPerMinute + " requests/minute per IP\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isRateLimited(String ip) {
        long bucket = System.currentTimeMillis() / 60_000L;
        String key = "rl:" + ip + ":" + bucket;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (Long.valueOf(1L).equals(count)) {
                // First request in this window — set TTL so the key expires automatically.
                // 2 minutes covers the current window plus a small buffer.
                redisTemplate.expire(key, Duration.ofMinutes(2));
            }
            return count != null && count > requestsPerMinute;
        } catch (Exception ex) {
            // Fail open: a Redis outage should not take the service down.
            log.warn("Redis unavailable for rate limiting ({}), allowing request through", ex.getMessage());
            return false;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For may contain a comma-separated chain of IPs;
            // the leftmost is the original client.
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

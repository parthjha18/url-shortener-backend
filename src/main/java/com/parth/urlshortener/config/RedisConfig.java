package com.parth.urlshortener.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache configuration.
 *
 * Key design decisions:
 *   - 10-minute TTL keeps hot data cached while avoiding stale entries.
 *   - JSON serialisation makes cache contents human-readable for debugging.
 *   - A custom {@link CacheErrorHandler} is registered so that Redis
 *     failures are logged but do NOT crash the application — the service
 *     falls back transparently to MySQL.
 */
@Configuration
@Slf4j
public class RedisConfig implements CachingConfigurer {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // Key prefix for readability in Redis CLI
                .prefixCacheNameWith("urlshortener::")
                // Cache entries expire after 10 minutes
                .entryTtl(Duration.ofMinutes(10))
                // Do not cache null values (missing short codes should always hit DB)
                .disableCachingNullValues()
                // Keys are stored as plain strings
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // Values are serialised as JSON for transparency
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Gracefully handle Redis errors so the application continues to serve
     * requests without caching rather than throwing 500s.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex,
                    org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis GET error for key [{}]: {}", key, ex.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException ex,
                    org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Redis PUT error for key [{}]: {}", key, ex.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex,
                    org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis EVICT error for key [{}]: {}", key, ex.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException ex,
                    org.springframework.cache.Cache cache) {
                log.warn("Redis CLEAR error: {}", ex.getMessage());
            }
        };
    }
}

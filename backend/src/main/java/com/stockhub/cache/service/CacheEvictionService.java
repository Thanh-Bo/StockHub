package com.stockhub.cache.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Service for evicting cache entries after ETL ingestion
 * or when underlying data changes.
 */
@Service
public class CacheEvictionService {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictionService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheEvictionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Evict all derived caches after an ETL run completes.
     * <p>
     * Patterns deleted:
     * <ul>
     *   <li>{@code screener::*} — Screener results depend on latest financials</li>
     *   <li>{@code comparison::*} — Comparison results depend on latest ratios</li>
     *   <li>{@code industry::*} — Industry averages depend on latest financials</li>
     *   <li>{@code financials::*} — Financial data cache</li>
     * </ul>
     * </p>
     */
    public void evictAfterETL() {
        log.info("Evicting caches after ETL ingestion");

        String[] patterns = {
                "screener::*",
                "comparison::*",
                "industry::*",
                "financials::*"
        };

        int totalDeleted = 0;
        for (String pattern : patterns) {
            int deleted = deleteByPattern(pattern);
            totalDeleted += deleted;
            log.debug("Deleted {} keys matching pattern '{}'", deleted, pattern);
        }

        log.info("ETL cache eviction completed: {} total keys deleted", totalDeleted);
    }

    /**
     * Evict all cache entries related to a specific company/ticker.
     *
     * @param ticker the company ticker symbol
     */
    public void evictCompanyCache(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return;
        }
        String pattern = "company::*" + ticker.toUpperCase() + "*";
        int deleted = deleteByPattern(pattern);
        log.debug("Evicted {} cache entries for company {}", deleted, ticker);
    }

    /**
     * Evict all cache entries for a specific user's watchlist.
     *
     * @param userId the user UUID
     */
    public void evictWatchlistCache(UUID userId) {
        if (userId == null) {
            return;
        }
        String pattern = "watchlist::" + userId.toString() + "*";
        int deleted = deleteByPattern(pattern);
        log.debug("Evicted {} cache entries for watchlist of user {}", deleted, userId);
    }

    /**
     * Delete all Redis keys matching the given pattern.
     * Uses SCAN to avoid blocking Redis on large key spaces.
     *
     * @param pattern the key pattern (supports glob-style wildcards)
     * @return number of keys deleted
     */
    public int deleteByPattern(String pattern) {
        Set<String> keys = scanKeys(pattern);
        if (keys.isEmpty()) {
            return 0;
        }
        Long deleted = redisTemplate.delete(keys);
        return deleted != null ? deleted.intValue() : 0;
    }

    /**
     * Scan Redis for keys matching a pattern using SCAN (non-blocking).
     *
     * @param pattern the key pattern
     * @return set of matching keys
     */
    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();

        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(options)) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("Error scanning Redis keys for pattern '{}': {}", pattern, e.getMessage());
        }

        return keys;
    }
}

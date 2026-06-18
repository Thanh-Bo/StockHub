package com.stockhub.company.service;

import com.stockhub.company.dto.CompanySearchResponse;
import com.stockhub.company.entity.Company;
import com.stockhub.company.repository.CompanyRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final String AUTOCOMPLETE_KEY_PREFIX = "search::autocomplete::";
    private static final String FULL_SEARCH_KEY_PREFIX = "search::full::";
    private static final String POPULAR_SEARCHES_KEY = "popular::searches";
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final int DEFAULT_AUTOCOMPLETE_LIMIT = 8;
    private static final int DEFAULT_TRENDING_LIMIT = 10;

    private final CompanyRepository companyRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public SearchService(CompanyRepository companyRepository,
                         RedisTemplate<String, Object> redisTemplate) {
        this.companyRepository = companyRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Autocomplete search using pg_trgm similarity, cached in Redis for 6 hours.
     */
    @SuppressWarnings("unchecked")
    public List<CompanySearchResponse> autocomplete(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        String normalizedQuery = query.trim().toLowerCase();
        String cacheKey = AUTOCOMPLETE_KEY_PREFIX + normalizedQuery;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) {
            return (List<CompanySearchResponse>) cached;
        }

        int effectiveLimit = limit > 0 ? limit : DEFAULT_AUTOCOMPLETE_LIMIT;
        Pageable pageable = PageRequest.of(0, effectiveLimit);
        List<Company> companies = companyRepository.autocomplete(normalizedQuery, pageable);

        List<CompanySearchResponse> results = companies.stream()
                .map(this::mapToSearchResponse)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, results, CACHE_TTL);
        return results;
    }

    /**
     * Full-text search using tsvector and trigram similarity, cached in Redis for 6 hours.
     */
    @SuppressWarnings("unchecked")
    public List<CompanySearchResponse> fullSearch(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        String normalizedQuery = query.trim().toLowerCase();
        String cacheKey = FULL_SEARCH_KEY_PREFIX + normalizedQuery;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) {
            return (List<CompanySearchResponse>) cached;
        }

        int effectiveLimit = limit > 0 ? limit : DEFAULT_TRENDING_LIMIT;
        Pageable pageable = PageRequest.of(0, effectiveLimit);
        List<Company> companies = companyRepository.fullSearch(normalizedQuery, pageable);

        List<CompanySearchResponse> results = companies.stream()
                .map(this::mapToSearchResponse)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, results, CACHE_TTL);
        return results;
    }

    /**
     * Record a search query by incrementing its score in the popular searches ZSET.
     */
    public void recordSearch(String query) {
        if (query == null || query.isBlank()) {
            return;
        }
        String normalizedQuery = query.trim().toLowerCase();
        redisTemplate.opsForZSet().incrementScore(POPULAR_SEARCHES_KEY, normalizedQuery, 1.0);
    }

    /**
     * Get trending searches (top N by score, descending).
     */
    public List<String> getTrendingSearches(int limit) {
        int effectiveLimit = limit > 0 ? limit : DEFAULT_TRENDING_LIMIT;
        Set<ZSetOperations.TypedTuple<Object>> topSearches =
                redisTemplate.opsForZSet().reverseRangeWithScores(
                        POPULAR_SEARCHES_KEY, 0, effectiveLimit - 1);

        if (topSearches == null) {
            return Collections.emptyList();
        }

        return topSearches.stream()
                .map(tuple -> tuple.getValue() != null ? tuple.getValue().toString() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private CompanySearchResponse mapToSearchResponse(Company company) {
        return new CompanySearchResponse(
                company.getId(),
                company.getTicker(),
                company.getName(),
                company.getSector(),
                company.getMarketCap()
        );
    }
}

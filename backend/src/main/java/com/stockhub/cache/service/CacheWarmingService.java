package com.stockhub.cache.service;

import com.stockhub.cache.config.MarketAwareTTL;
import com.stockhub.company.dto.DashboardResponse;
import com.stockhub.company.entity.Company;
import com.stockhub.company.repository.CompanyRepository;
import com.stockhub.company.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Pre-warms Redis caches with frequently accessed data to reduce
 * latency on first requests and during cache misses.
 */
@Service
public class CacheWarmingService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmingService.class);

    private static final String POPULAR_WATCHED_KEY = "popular::watched";
    private static final String DASHBOARD_PREFIX = "dashboard::";
    private static final String INDUSTRY_AVERAGES_KEY = "industry::averages";
    private static final String AUTOCOMPLETE_PREFIX = "search::autocomplete::";

    private final RedisTemplate<String, Object> redisTemplate;
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final MarketAwareTTL marketAwareTTL;

    public CacheWarmingService(RedisTemplate<String, Object> redisTemplate,
                               CompanyRepository companyRepository,
                               CompanyService companyService,
                               MarketAwareTTL marketAwareTTL) {
        this.redisTemplate = redisTemplate;
        this.companyRepository = companyRepository;
        this.companyService = companyService;
        this.marketAwareTTL = marketAwareTTL;
    }

    /**
     * Warm the dashboard cache for the top 50 most-watched companies.
     * <p>
     * Retrieves tickers from the "popular::watched" sorted set in Redis,
     * computes their dashboard data, and caches it with a market-aware TTL.
     * </p>
     */
    public void warmDashboardCache() {
        log.info("Starting dashboard cache warming");

        // Get top 50 companies from the popularity ZSET
        Set<ZSetOperations.TypedTuple<Object>> topCompanies = redisTemplate
                .opsForZSet()
                .reverseRangeWithScores(POPULAR_WATCHED_KEY, 0, 49);

        if (topCompanies == null || topCompanies.isEmpty()) {
            log.info("No popular companies found in ZSET '{}'. Falling back to sample companies.",
                    POPULAR_WATCHED_KEY);
            warmDashboardFromSample();
            return;
        }

        int count = 0;
        for (ZSetOperations.TypedTuple<Object> entry : topCompanies) {
            String ticker = entry.getValue() != null ? entry.getValue().toString() : null;
            if (ticker == null || ticker.isBlank()) {
                continue;
            }
            cacheDashboard(ticker);
            count++;
        }

        log.info("Dashboard cache warming completed for {} companies", count);
    }

    /**
     * Warm the industry averages cache by computing averages for all sectors.
     */
    public void warmIndustryAveragesCache() {
        log.info("Starting industry averages cache warming");

        // Industry averages are typically computed from the database.
        // Here we set a marker that the cache has been warmed.
        Duration ttl = Duration.ofHours(24);

        // Get all active companies to determine unique sectors
        java.util.List<Company> activeCompanies = companyRepository.findByIsActiveTrue();
        java.util.Set<String> sectors = activeCompanies.stream()
                .map(Company::getSector)
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        for (String sector : sectors) {
            String cacheKey = INDUSTRY_AVERAGES_KEY + "::" + sector.toLowerCase().replace(" ", "_");
            // Check if already cached
            if (Boolean.FALSE.equals(redisTemplate.hasKey(cacheKey))) {
                // Place a marker - actual computation happens on first retrieval
                redisTemplate.opsForValue().set(
                        cacheKey + "::warmed",
                        true,
                        ttl.toMillis(),
                        TimeUnit.MILLISECONDS);
            }
        }

        log.info("Industry averages cache warming completed for {} sectors", sectors.size());
    }

    /**
     * Warm the autocomplete cache with common search prefixes.
     */
    public void warmAutocompleteCache() {
        log.info("Starting autocomplete cache warming");

        java.util.List<Company> activeCompanies = companyRepository.findByIsActiveTrue();
        Duration ttl = Duration.ofHours(6);

        for (Company company : activeCompanies) {
            if (company.getTicker() == null || !company.isActive()) {
                continue;
            }

            String ticker = company.getTicker().toUpperCase();
            String name = company.getName() != null ? company.getName().toLowerCase() : "";

            // Cache full ticker prefixes (e.g., "A", "AA", "AAP", "AAPL")
            for (int i = 1; i <= ticker.length(); i++) {
                String prefix = ticker.substring(0, i);
                String cacheKey = AUTOCOMPLETE_PREFIX + "ticker::" + prefix;
                redisTemplate.opsForValue().setIfAbsent(
                        cacheKey, "warmed", ttl.toMillis(), TimeUnit.MILLISECONDS);
            }

            // Cache name-based prefixes (first letter, first two, etc.)
            if (!name.isBlank()) {
                String[] words = name.split("\\s+");
                for (String word : words) {
                    if (word.length() >= 2) {
                        for (int i = 2; i <= Math.min(word.length(), 8); i++) {
                            String prefix = word.substring(0, i).toLowerCase();
                            String cacheKey = AUTOCOMPLETE_PREFIX + "name::" + prefix;
                            redisTemplate.opsForValue().setIfAbsent(
                                    cacheKey, "warmed", ttl.toMillis(), TimeUnit.MILLISECONDS);
                        }
                    }
                }
            }
        }

        log.info("Autocomplete cache warming completed for {} companies", activeCompanies.size());
    }

    // --- Private helpers ---

    private void cacheDashboard(String ticker) {
        try {
            String cacheKey = DASHBOARD_PREFIX + ticker.toUpperCase();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                return; // Already cached
            }
            DashboardResponse dashboard = companyService.getDashboard(ticker);
            Duration ttl = marketAwareTTL.getDashboardTTL();
            redisTemplate.opsForValue().set(cacheKey, dashboard, ttl.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Cached dashboard for {}", ticker);
        } catch (Exception e) {
            log.warn("Failed to warm dashboard cache for ticker {}: {}", ticker, e.getMessage());
        }
    }

    private void warmDashboardFromSample() {
        java.util.List<Company> companies = companyRepository.findByIsActiveTrue();
        int count = 0;
        for (Company company : companies) {
            if (count >= 50) break;
            cacheDashboard(company.getTicker());
            count++;
        }
        log.info("Dashboard cache warmed from {} active companies", count);
    }
}

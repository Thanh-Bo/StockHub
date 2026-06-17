# Section 12: Redis Caching Strategy

## 12.1 Cache Hierarchy

```
┌─────────────────────────────────────────────────────────────────┐
│                      REDIS CACHE LAYERS                          │
│                                                                  │
│  ┌─────────────────────┐  ┌─────────────────────┐               │
│  │   L1: Application   │  │   L2: Session &      │               │
│  │       Data Cache     │  │       Auth Cache      │               │
│  │                     │  │                     │               │
│  │ • Company profiles  │  │ • Refresh tokens    │               │
│  │ • Dashboard data    │  │ • JWK sets          │               │
│  │ • Screener results  │  │ • Rate limits       │               │
│  │ • Industry averages │  │ • Login attempts    │               │
│  │ • Search results    │  │ • Email verify      │               │
│  │ • Popular tickers   │  │ • Password reset    │               │
│  └─────────────────────┘  └─────────────────────┘               │
│                                                                  │
│  ┌─────────────────────┐  ┌─────────────────────┐               │
│  │   L3: Real-time      │  │   L4: Analytics      │               │
│  │       Data            │  │       Data           │               │
│  │                     │  │                     │               │
│  │ • Latest prices     │  │ • Trending searches │               │
│  │ • Market status     │  │ • Popular stocks    │               │
│  │                     │  │ • Search analytics  │               │
│  └─────────────────────┘  └─────────────────────┘               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 12.2 Cache Key Design

### Naming Convention
```
{domain}::{subdomain}::{identifier}
```

### Complete Cache Key Catalog

#### Company & Dashboard
| Key | Type | TTL | Description |
|-----|------|-----|-------------|
| `company::profile::{ticker}` | Hash | 24h | Full company profile |
| `company::dashboard::{ticker}` | Hash | 1h (market) / 24h (off) | Aggregated dashboard data |
| `company::metrics::{ticker}` | Hash | 1h | Latest financial ratios |
| `company::exists::{ticker}` | String (bool) | 1h | Quick existence check |

#### Financial Statements
| Key | Type | TTL | Description |
|-----|------|-----|-------------|
| `financials::income::{ticker}::annual::5y` | String (JSON) | 24h | 5 years annual income statements |
| `financials::balance::{ticker}::annual::5y` | String (JSON) | 24h | 5 years annual balance sheets |
| `financials::cashflow::{ticker}::annual::5y` | String (JSON) | 24h | 5 years annual cash flows |

#### Prices
| Key | Type | TTL | Description |
|-----|------|-----|-------------|
| `prices::{ticker}::1Y::1d` | String (JSON) | 1h | 1 year daily prices |
| `prices::{ticker}::latest` | Hash | 5 min | Latest price snapshot |

#### Screener
| Key | Type | TTL | Description |
|-----|------|-----|-------------|
| `screener::results::{filterHash}` | String (JSON) | 1h | Cached screener results |
| `screener::filters::metadata` | String (JSON) | 24h | Available filter definitions |

#### Search
| Key | Type | TTL | Description |
|-----|------|-----|-------------|
| `search::autocomplete::{query}` | List | 6h | Autocomplete results |
| `search::full::{query}` | String (JSON) | 6h | Full search results |
| `popular::searches` | Sorted Set | — | Trending searches (scored) |

#### Industry
| Key | Type | TTL | Description |
|-----|------|-----|-------------|
| `industry::list` | String (JSON) | 24h | All industries |
| `industry::averages::{sector}::{industry}` | String (JSON) | 24h | Industry averages |

#### Comparison
| Key | Type | TTL | Description |
|-----|------|-----|-------------|
| `comparison::{tickerHash}` | String (JSON) | 1h | Cached comparison results |

#### Auth & Session
| Key | Type | TTL | Description |
|-----|------|-----|-------------|
| `refresh_token::{tokenId}` | Hash | 7d | Refresh token data |
| `user_tokens::{userId}` | Set | 7d | Active tokens for user |
| `blacklist::{tokenId}` | String | remaining TTL | Revoked token |
| `rate_limit::{ip}::{endpoint}` | Integer | varies | Rate limit counter |
| `login_attempts::{ip}` | Integer | 5 min | Failed login counter |
| `email_verify::{token}` | Hash | 24h | Email verification data |

---

## 12.3 TTL Strategy

### Market-Aware TTL

```java
@Component
public class MarketAwareTTL {

    // NYSE market hours: 9:30 AM - 4:00 PM ET
    public Duration getDashboardTTL() {
        if (isMarketOpen()) {
            return Duration.ofHours(1);   // More frequent refresh during trading
        }
        return Duration.ofHours(24);       // Stale data acceptable when closed
    }

    public Duration getPriceTTL() {
        if (isMarketOpen()) {
            return Duration.ofMinutes(5);  // Refresh during trading
        }
        return Duration.ofHours(24);        // Stale after close
    }

    private boolean isMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        DayOfWeek day = now.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;

        LocalTime time = now.toLocalTime();
        return !time.isBefore(LocalTime.of(9, 30))
            && !time.isAfter(LocalTime.of(16, 0));
    }
}
```

---

## 12.4 Cache Eviction Strategy

### Write-Through on Updates

```java
@Service
public class CacheEvictionService {

    // Called after nightly ETL completes
    public void evictAfterETL() {
        // Flush all screener-related caches (data changed)
        deleteByPattern("screener::*");
        deleteByPattern("comparison::*");
        deleteByPattern("industry::*");
        deleteByPattern("financials::*");

        // Don't flush company profiles — those rarely change
        // Don't flush search — tickers don't change
    }

    // Called when user modifies watchlist
    @CacheEvict(value = "watchlist", key = "#userId")
    public void evictWatchlistCache(UUID userId) {}

    // Called when admin updates company data manually
    public void evictCompanyCache(String ticker) {
        deleteByPattern("company::*" + ticker + "*");
        deleteByPattern("dashboard::" + ticker);
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

### Scheduled Eviction

```java
// Every morning at 4:00 AM ET (after ETL finishes)
@Scheduled(cron = "0 0 4 * * ?", zone = "America/New_York")
public void dailyCacheCleanup() {
    evictAfterETL();
    log.info("Daily cache cleanup completed. Keys evicted: {}", evictedCount);
}
```

---

## 12.5 Cache Warming

### After ETL Completion

```java
@Service
public class CacheWarmingService {

    private final CompanyRepository companyRepo;
    private final DashboardService dashboardService;
    private final IndustryService industryService;
    private final RedisTemplate<String, Object> redisTemplate;

    public void warmCache() {
        warmPopularDashboards();
        warmIndustryAverages();
        warmAutocompleteCache();
    }

    // Warm dashboards for top 50 most-watched companies
    private void warmPopularDashboards() {
        List<String> topTickers = redisTemplate.opsForZSet()
            .reverseRange("popular::watched", 0, 49);

        topTickers.parallelStream().forEach(ticker -> {
            try {
                DashboardResponse dashboard = dashboardService.getDashboard(ticker);
                String key = "company::dashboard::" + ticker;
                redisTemplate.opsForValue().set(key, dashboard,
                    marketAwareTTL.getDashboardTTL());
            } catch (Exception e) {
                log.warn("Failed to warm dashboard cache for {}", ticker, e);
            }
        });
    }

    // Warm industry averages for all sectors
    private void warmIndustryAverages() {
        List<Industry> industries = industryService.getAllIndustries();
        industries.parallelStream().forEach(ind -> {
            IndustryAveragesResponse averages = industryService
                .getAverages(ind.getSector(), ind.getIndustry());
            String key = String.format("industry::averages::%s::%s",
                ind.getSector(), ind.getIndustry());
            redisTemplate.opsForValue().set(key, averages, Duration.ofHours(24));
        });
    }
}
```

---

## 12.6 Cache-Aside Pattern Implementation

```java
@Service
public class CompanyService {

    // Spring @Cacheable with custom Redis cache manager
    @Cacheable(value = "company::profile", key = "#ticker",
               unless = "#result == null")
    public CompanyResponse getCompanyProfile(String ticker) {
        Company company = companyRepo.findByTicker(ticker)
            .orElseThrow(() -> new CompanyNotFoundException(ticker));
        return companyMapper.toResponse(company);
    }

    // Manual cache-aside for complex dashboard assembly
    public DashboardResponse getDashboard(String ticker) {
        String cacheKey = "company::dashboard::" + ticker;

        // 1. Check cache
        DashboardResponse cached = (DashboardResponse) redisTemplate
            .opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 2. Cache miss — assemble from DB
        DashboardResponse dashboard = assembleDashboard(ticker);

        // 3. Store in cache
        Duration ttl = marketAwareTTL.getDashboardTTL();
        redisTemplate.opsForValue().set(cacheKey, dashboard, ttl);

        return dashboard;
    }
}
```

---

## 12.7 Memory Budget (Upstash Free Tier)

Upstash free tier: 256 MB, 1 database.

| Cache Category | Estimated Size | % of Budget |
|---------------|---------------|-------------|
| Dashboard data (100 companies) | ~20 MB | 8% |
| Financial statements (500 companies) | ~50 MB | 20% |
| Screener results (50 combinations) | ~10 MB | 4% |
| Search autocomplete (200 queries) | ~5 MB | 2% |
| Industry averages | ~2 MB | 1% |
| Auth tokens (500 users × 3 tokens) | ~5 MB | 2% |
| Rate limiting | ~1 MB | <1% |
| **Total** | **~93 MB** | **36%** |

Well within the 256 MB free tier. Headroom for growth to Russell 3000.

---

## 12.8 Redis Configuration

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // JSON serialization for values
        Jackson2JsonRedisSerializer<Object> serializer =
            new Jackson2JsonRedisSerializer<>(Object.class);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder().build(),
            ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("company::profile",
                config.entryTtl(Duration.ofHours(24)))
            .withCacheConfiguration("screener::results",
                config.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("search::autocomplete",
                config.entryTtl(Duration.ofHours(6)))
            .build();
    }
}
```

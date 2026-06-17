# Section 20: Deployment Architecture

## 20.1 Production Infrastructure

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          PRODUCTION DEPLOYMENT                           │
│                                                                          │
│  ┌─────────────────────────────────────────┐                            │
│  │              DNS / HTTPS                 │                            │
│  │         stockhub.com (Cloudflare)        │                            │
│  │         api.stockhub.com                 │                            │
│  └─────────────┬───────────────────────────┘                            │
│                │                                                         │
│    ┌───────────┴───────────┐                                            │
│    │                       │                                            │
│    ▼                       ▼                                            │
│  ┌──────────────────┐  ┌──────────────────────────────┐                 │
│  │  Vercel (Frontend)│  │  Azure App Service (Backend) │                 │
│  │                  │  │                              │                 │
│  │  Angular 19 SPA  │  │  Region: East US 2           │                 │
│  │  Custom Domain   │  │  Plan: B1 (Basic)            │                 │
│  │  Auto-SSL        │  │  1 Core, 1.75 GB RAM         │                 │
│  │  Global CDN      │  │  Docker Container            │                 │
│  │                  │  │  Java 21, Spring Boot        │                 │
│  │  Cost: FREE      │  │                              │                 │
│  │  (Hobby plan)    │  │  Auto-Scale: Up to 3 inst.   │                 │
│  └──────────────────┘  │  Cost: ~$13/month             │                 │
│                        └──────────────┬───────────────┘                 │
│                                       │                                  │
│                    ┌──────────────────┼──────────────────┐              │
│                    │                  │                  │              │
│                    ▼                  ▼                  ▼              │
│          ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│          │  Supabase     │  │   Upstash    │  │  GitHub       │          │
│          │  (PostgreSQL) │  │   (Redis)    │  │  Container    │          │
│          │              │  │              │  │  Registry     │          │
│          │  Postgres 16  │  │  Redis 7     │  │              │          │
│          │  TimescaleDB  │  │  256 MB      │  │  Docker       │          │
│          │  Extension    │  │  SSL/TLS     │  │  Images       │          │
│          │              │  │              │  │              │          │
│          │  500 MB DB    │  │  Daily       │  │  Private      │          │
│          │  Auto-backups │  │  Backups     │  │  Registry     │          │
│          │              │  │              │  │              │          │
│          │  Cost: FREE   │  │  Cost: FREE  │  │  Cost: FREE  │          │
│          └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────┐       │
│  │                     MONITORING STACK                          │       │
│  │                                                              │       │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │       │
│  │  │ Application │  │   Uptime    │  │   Error Tracking    │  │       │
│  │  │  Monitoring │  │  Monitoring │  │                     │  │       │
│  │  │             │  │             │  │                     │  │       │
│  │  │ Spring      │  │  Azure      │  │  Sentry (free tier) │  │       │
│  │  │ Actuator    │  │  App        │  │  Backend errors     │  │       │
│  │  │ Prometheus  │  │  Insights   │  │  Frontend errors    │  │       │
│  │  │ Metrics     │  │             │  │                     │  │       │
│  │  │             │  │             │  │                     │  │       │
│  │  │ Cost: FREE  │  │  Cost: FREE │  │  Cost: FREE         │  │       │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │       │
│  └──────────────────────────────────────────────────────────────┘       │
│                                                                          │
│  TOTAL MONTHLY COST: ~$13/month (Azure App Service B1 only)             │
│  Everything else: Free tiers                                             │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 20.2 Cost Breakdown

| Service | Plan | Monthly Cost | Notes |
|---------|------|-------------|-------|
| **Azure App Service** | B1 (Basic) | ~$13 | 1 core, 1.75 GB RAM, custom domain |
| **Vercel** | Hobby | $0 | 100 GB bandwidth, auto-SSL |
| **Supabase** | Free | $0 | 500 MB DB, 2 GB bandwidth, auto-backups |
| **Upstash Redis** | Free | $0 | 256 MB, 10K commands/day |
| **GitHub Container Registry** | Free | $0 | Public repos, 2 GB storage |
| **GitHub Actions** | Free | $0 | 2,000 min/month |
| **Cloudflare** | Free | $0 | DNS, CDN, DDoS protection |
| **Sentry** | Free | $0 | 5K errors/month |
| **TOTAL** | | **~$13/month** | |

---

## 20.3 Environment Variables by Environment

### Development (localhost)

```properties
# application-dev.yml
spring.datasource.url=jdbc:postgresql://localhost:5432/stockhub_dev
spring.datasource.username=stockhub
spring.datasource.password=devpassword
spring.redis.host=localhost
spring.redis.port=6379

stockhub:
  jwt:
    private-key: classpath:keys/dev-private.pem
    public-key: classpath:keys/dev-public.pem
  ingestion:
    enabled: false  # Disable scheduled ETL in dev
```

### Production (Azure)

```properties
# application-prod.yml — loaded from environment variables
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.redis.url=${REDIS_URL}
spring.redis.password=${REDIS_PASSWORD}

stockhub:
  jwt:
    private-key: ${JWT_PRIVATE_KEY}
    public-key: ${JWT_PUBLIC_KEY}
  google:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
  ingestion:
    enabled: true
  cors:
    allowed-origins: https://stockhub.com
```

---

## 20.4 Monitoring & Observability

### Spring Boot Actuator Endpoints

| Endpoint | Purpose | Exposed? |
|----------|---------|----------|
| `/actuator/health` | Liveness probe (Azure uses this) | Yes |
| `/actuator/health/readiness` | Readiness probe (DB + Redis connected) | Yes |
| `/actuator/metrics` | Prometheus-compatible metrics | Admin only |
| `/actuator/prometheus` | Prometheus scrape endpoint | Admin only |
| `/actuator/info` | Application version, git commit | Yes |

### Key Metrics (Micrometer + Prometheus)

```java
// Custom metrics
@Bean
public MeterBinder customMetrics() {
    return registry -> {
        // ETL metrics
        Counter.builder("etl.companies.processed")
            .description("Companies processed by ETL")
            .register(registry);

        // API metrics
        Timer.builder("api.dashboard.load.time")
            .description("Dashboard load time")
            .register(registry);

        // Cache metrics (Spring Cache auto-registers)
        // Business metrics
        Gauge.builder("users.registered", userRepo::count)
            .description("Total registered users")
            .register(registry);
    };
}
```

### Structured Logging

```yaml
logging:
  pattern:
    console: '{"timestamp":"%d{ISO8601}","level":"%p","logger":"%c","message":"%m","traceId":"%X{traceId}"}%n'
  level:
    com.stockhub: INFO
    com.stockhub.ingestion: DEBUG
    org.springframework.security: WARN
```

**Logs shipped to**: Azure App Service Log Stream (free) + Application Insights (optional upgrade).

---

## 20.5 Backup Strategy

| Component | Backup Method | Frequency | Retention |
|-----------|--------------|-----------|-----------|
| Supabase PostgreSQL | Automatic backups (managed) | Daily | 7 days |
| Upstash Redis | Automatic backups (managed) | Daily | 1 day |
| Docker Images | GitHub Container Registry | Per deployment | All tags kept |
| Source Code | GitHub | Per commit | Forever |

### Manual Backup (Before Major Changes)

```bash
# PostgreSQL dump (one-off)
pg_dump $DB_URL > stockhub_backup_$(date +%Y%m%d).sql

# Redis snapshot (automatic via Upstash, but can trigger manually)
redis-cli -h $REDIS_HOST -a $REDIS_PASSWORD BGSAVE
```

---

## 20.6 Scaling Plan (If/When Needed)

| Trigger | Action |
|---------|--------|
| 50+ concurrent users | Scale Azure App Service to B2 (2 cores, 3.5 GB) |
| DB CPU > 80% sustained | Upgrade Supabase to Pro ($25/month) |
| Redis memory > 80% | Upgrade Upstash to Pay-as-you-go |
| S&P 500 → Russell 3000 | Add more RAM to App Service; TimescaleDB handles time-series |
| Global users | Add Azure Front Door CDN for API caching |

---

## 20.7 Disaster Recovery

| Scenario | Recovery |
|----------|----------|
| Database corrupted | Restore from Supabase daily backup (< 2 hours) |
| Redis data lost | Cache is ephemeral — warm on next ETL run |
| Azure region outage | Redeploy to another region (IaC scripts in repo) |
| Accidental delete | GitHub history → revert commit; DB → point-in-time recovery |
| Secrets leaked | Rotate all keys, invalidate all JWT tokens |

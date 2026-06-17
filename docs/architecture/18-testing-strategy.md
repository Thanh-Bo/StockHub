# Section 18: Testing Strategy

## 18.1 Testing Pyramid

```
         ╱───────╲
        ╱   E2E   ╲           5%  — Playwright (critical user journeys)
       ╱───────────╲
      ╱     API     ╲         15% — Spring MockMvc / WebTestClient
     ╱───────────────╲
    ╱   Integration    ╲      30% — @SpringBootTest + Testcontainers
   ╱───────────────────╲
  ╱     Unit Tests       ╲    50% — JUnit 5 + Mockito
 ╱─────────────────────────╲
```

---

## 18.2 Unit Tests (50% of test suite)

### What to Unit Test

- Service layer business logic
- Metric calculation formulas
- Validation rules
- DTO mapping (MapStruct)
- Utility classes
- Filter/specification builders

### Example: Metric Calculation Service

```java
@ExtendWith(MockitoExtension.class)
class MetricCalculationServiceTest {

    @Mock private IncomeStatementRepository incomeRepo;
    @Mock private BalanceSheetRepository balanceRepo;
    @Mock private CashFlowStatementRepository cashFlowRepo;
    @Mock private FinancialRatioRepository ratioRepo;
    @Mock private PriceRepository priceRepo;
    @Mock private CompanyRepository companyRepo;

    @InjectMocks
    private MetricCalculationService service;

    @Test
    void shouldCalculateRevenueGrowthCorrectly() {
        IncomeStatement current = IncomeStatement.builder()
            .totalRevenue(new BigDecimal("120000000"))
            .build();
        IncomeStatement previous = IncomeStatement.builder()
            .totalRevenue(new BigDecimal("100000000"))
            .build();

        BigDecimal growth = service.calculateRevenueGrowthYoY(current, previous);

        assertThat(growth).isEqualByComparingTo(new BigDecimal("20.0000"));
    }

    @Test
    void shouldReturnNullWhenPreviousYearIsMissing() {
        IncomeStatement current = IncomeStatement.builder()
            .totalRevenue(new BigDecimal("120000000"))
            .build();

        BigDecimal growth = service.calculateRevenueGrowthYoY(current, null);

        assertThat(growth).isNull();
    }

    @Test
    void shouldHandleNegativeEarningsForPE() {
        BigDecimal stockPrice = new BigDecimal("100");
        BigDecimal eps = new BigDecimal("-5.00");

        BigDecimal pe = service.calculatePE(stockPrice, eps);

        assertThat(pe).isNull(); // P/E not meaningful for negative earnings
    }

    @Test
    void shouldCalculateROECorrectly() {
        IncomeStatement income = IncomeStatement.builder()
            .netIncome(new BigDecimal("50000000"))
            .build();
        BalanceSheet current = BalanceSheet.builder()
            .totalShareholderEquity(new BigDecimal("200000000"))
            .build();
        BalanceSheet previous = BalanceSheet.builder()
            .totalShareholderEquity(new BigDecimal("180000000"))
            .build();

        BigDecimal roe = service.calculateROE(income, current, previous);
        // ROE = 50M / ((200M + 180M) / 2) = 50M / 190M = 26.3158%

        assertThat(roe).isEqualByComparingTo(new BigDecimal("26.3158"));
    }
}
```

### Example: Watchlist Validation

```java
@Test
void shouldRejectDuplicateStocksInWatchlist() {
    UUID watchlistId = UUID.randomUUID();
    when(watchlistStockRepo.existsByWatchlistIdAndCompanyId(watchlistId, companyId))
        .thenReturn(true);

    assertThatThrownBy(() -> watchlistService.addStock(watchlistId, userId, "AAPL"))
        .isInstanceOf(DuplicateStockException.class)
        .hasMessageContaining("already in this watchlist");
}

@Test
void shouldEnforceFreeTierWatchlistLimit() {
    User freeUser = User.builder().role(UserRole.FREE).build();
    when(userRepo.findById(userId)).thenReturn(Optional.of(freeUser));
    when(watchlistStockRepo.countByWatchlistId(watchlistId)).thenReturn(10L);

    assertThatThrownBy(() -> watchlistService.addStock(watchlistId, userId, "MSFT"))
        .isInstanceOf(WatchlistLimitExceededException.class)
        .hasMessageContaining("Upgrade to Premium");
}
```

---

## 18.3 Integration Tests (30%)

### Repository Tests (with Testcontainers PostgreSQL)

```java
@SpringBootTest
@Testcontainers
class CompanySearchRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("stockhub_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CompanySearchRepository searchRepo;

    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void setUp() {
        em.persist(Company.builder()
            .ticker("AAPL").name("Apple Inc.")
            .sector("Technology").isActive(true).build());
        em.persist(Company.builder()
            .ticker("MSFT").name("Microsoft Corporation")
            .sector("Technology").isActive(true).build());
        em.flush();
    }

    @Test
    void shouldFindByExactTicker() {
        List<CompanySearchProjection> results = searchRepo.searchCompanies("AAPL", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTicker()).isEqualTo("AAPL");
    }

    @Test
    void shouldFindByPartialName() {
        List<CompanySearchProjection> results = searchRepo.searchCompanies("apple", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).contains("Apple");
    }

    @Test
    void shouldReturnEmptyForNoMatch() {
        List<CompanySearchProjection> results = searchRepo.searchCompanies("zzzzz", 10);

        assertThat(results).isEmpty();
    }
}
```

### Service Integration Tests

```java
@SpringBootTest
@Transactional
class DashboardServiceIntegrationTest {

    @Autowired private DashboardService dashboardService;
    @Autowired private TestEntityManager em;

    @Test
    void shouldAssembleCompleteDashboard() {
        // Given: Company with price, financials, ratios
        Company apple = em.persist(Company.builder()
            .ticker("AAPL").name("Apple Inc.")
            .sector("Technology").marketCap(new BigDecimal("2800000000000"))
            .build());

        em.persist(StockPrice.builder()
            .companyId(apple.getId()).date(LocalDate.now())
            .close(new BigDecimal("178.50"))
            .build());

        em.persist(createIncomeStatement(apple.getId(), "2025-09-30", 383_285_000_000L));
        em.persist(createFinancialRatio(apple.getId(), "2025-09-30"));

        em.flush();

        // When
        DashboardResponse response = dashboardService.getDashboard("AAPL");

        // Then
        assertThat(response.getTicker()).isEqualTo("AAPL");
        assertThat(response.getMarketCap()).isEqualByComparingTo("2800000000000");
        assertThat(response.getCurrentPrice()).isEqualByComparingTo("178.50");
        assertThat(response.getRoe()).isNotNull();
    }
}
```

---

## 18.4 API Tests (15%)

### Controller Tests with MockMvc

```java
@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CompanyService companyService;

    @Test
    void shouldReturnCompanyProfile() throws Exception {
        CompanyResponse response = new CompanyResponse(
            UUID.randomUUID(), "AAPL", "Apple Inc.",
            "Technology", "Consumer Electronics",
            new BigDecimal("2800000000000"), true
        );
        when(companyService.getCompanyProfile("AAPL")).thenReturn(response);

        mockMvc.perform(get("/api/v1/companies/AAPL")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticker").value("AAPL"))
            .andExpect(jsonPath("$.name").value("Apple Inc."))
            .andExpect(jsonPath("$.marketCap").value(2800000000000L));
    }

    @Test
    void shouldReturn404ForUnknownTicker() throws Exception {
        when(companyService.getCompanyProfile("ZZZZ"))
            .thenThrow(new CompanyNotFoundException("ZZZZ"));

        mockMvc.perform(get("/api/v1/companies/ZZZZ")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Company not found"))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn401ForUnauthenticatedWatchlistAccess() throws Exception {
        mockMvc.perform(get("/api/v1/watchlists")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }
}
```

### API Integration Tests (Full Stack)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class ScreenerApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired private MockMvc mockMvc;
    @Autowired private TestEntityManager em;

    @BeforeEach
    void seedData() {
        // Seed 50 companies with varying metrics
        for (int i = 0; i < 50; i++) {
            Company c = em.persist(Company.builder()
                .ticker("STK" + i).name("Stock " + i)
                .sector(i < 25 ? "Technology" : "Healthcare")
                .marketCap(BigDecimal.valueOf(10_000_000_000L + i * 5_000_000_000L))
                .build());

            em.persist(FinancialRatio.builder()
                .companyId(c.getId())
                .periodType(PeriodType.ANNUAL)
                .fiscalDateEnding(LocalDate.of(2025, 12, 31))
                .peRatio(BigDecimal.valueOf(15 + i))
                .revenueGrowthYoY(BigDecimal.valueOf(5 + i * 0.5))
                .roe(BigDecimal.valueOf(10 + i * 2))
                .build());
        }
        em.flush();
    }

    @Test
    void shouldFilterAndSortResults() throws Exception {
        String requestBody = """
            {
              "filters": [
                {"field": "sector", "operator": "IN", "values": ["Technology"]},
                {"field": "peRatio", "operator": "BETWEEN", "minValue": "20", "maxValue": "40"}
              ],
              "sort": {"field": "marketCap", "direction": "DESC"},
              "pagination": {"page": 0, "size": 10}
            }
            """;

        mockMvc.perform(post("/api/v1/screener/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(10))
            .andExpect(jsonPath("$.totalElements").isNumber())
            .andExpect(jsonPath("$.content[0].sector").value("Technology"));
    }
}
```

---

## 18.5 End-to-End Tests (5%)

### Playwright Tests (Critical User Journeys)

```typescript
// e2e/dashboard.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Company Dashboard', () => {
  test('should display AAPL dashboard with key metrics', async ({ page }) => {
    await page.goto('/stocks/AAPL');

    // Company header
    await expect(page.locator('[data-testid="company-name"]')).toContainText('Apple');
    await expect(page.locator('[data-testid="current-price"]')).toBeVisible();

    // Price chart
    await expect(page.locator('[data-testid="price-chart"] canvas')).toBeVisible();

    // Key metrics
    await expect(page.locator('[data-testid="metric-market-cap"]')).toBeVisible();
    await expect(page.locator('[data-testid="metric-pe-ratio"]')).toBeVisible();

    // Financial tabs
    await page.locator('[data-testid="tab-financials"]').click();
    await expect(page.locator('[data-testid="financial-table"]')).toBeVisible();
  });

  test('should search for a company', async ({ page }) => {
    await page.goto('/');
    await page.locator('[data-testid="global-search"]').fill('apple');
    await expect(page.locator('[data-testid="autocomplete-dropdown"]')).toBeVisible();
    await expect(page.locator('[data-testid="autocomplete-item"]').first()).toContainText('AAPL');

    await page.locator('[data-testid="autocomplete-item"]').first().click();
    await expect(page).toHaveURL(/\/stocks\/AAPL/);
  });
});

test.describe('Watchlist (Authenticated)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/auth/login');
    await page.locator('[data-testid="email-input"]').fill('test@example.com');
    await page.locator('[data-testid="password-input"]').fill('password123');
    await page.locator('[data-testid="login-button"]').click();
    await expect(page).toHaveURL('/watchlists');
  });

  test('should add stock to watchlist', async ({ page }) => {
    await page.locator('[data-testid="add-stock-button"]').click();
    await page.locator('[data-testid="add-stock-input"]').fill('MSFT');
    await page.locator('[data-testid="confirm-add-stock"]').click();
    await expect(page.locator('[data-testid="watchlist-stock-MSF"]')).toBeVisible();
  });
});
```

---

## 18.6 Test Coverage Targets

| Layer | Coverage Target | What's Excluded |
|-------|----------------|-----------------|
| Service classes | 90%+ | Simple getters/setters |
| Metric calculation | 100% | — (every formula tested with edge cases) |
| Validation | 95%+ | Boilerplate |
| Controllers | 80%+ | (tested via API integration, not pure unit) |
| Repositories | 80%+ | Spring Data generated methods (tested implicitly) |
| DTOs / Mappers | 60%+ | MapStruct-generated mapping |
| Configuration | 50%+ | Spring config classes |

---

## 18.7 Test Execution in CI

```yaml
# tests are run in parallel groups
test-groups:
  - unit-tests:           ~30 seconds   (no DB, no containers)
  - repository-tests:     ~2 minutes    (Testcontainers PostgreSQL)
  - integration-tests:    ~3 minutes    (Testcontainers PostgreSQL + Redis)
  - api-tests:            ~2 minutes    (MockMvc, no containers)
  - e2e-tests:            ~3 minutes    (Playwright, requires built app)
```

Tests run on every push to `main` and every PR. Repository and integration tests use Testcontainers — no need for a shared test database.

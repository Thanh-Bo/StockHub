package com.stockhub.comparison.service;

import com.stockhub.comparison.dto.IndustryAveragesResponse;
import com.stockhub.company.entity.Industry;
import com.stockhub.company.repository.IndustryRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class IndustryService {

    private static final String CACHE_PREFIX_AVG = "industry::averages::";
    private static final String CACHE_PREFIX_ALL = "industry::all";
    private static final long CACHE_TTL_HOURS = 24;
    private static final String MV_INDUSTRY_AVERAGES = "mv_industry_averages";

    private final IndustryRepository industryRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public IndustryService(IndustryRepository industryRepository,
                           JdbcTemplate jdbcTemplate,
                           RedisTemplate<String, Object> redisTemplate) {
        this.industryRepository = industryRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Get industry averages for a sector and industry combination.
     * Cached in Redis for 24 hours.
     */
    @SuppressWarnings("unchecked")
    public IndustryAveragesResponse getAverages(String sector, String industry) {
        String cacheKey = CACHE_PREFIX_AVG + sector + "::" + industry;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof IndustryAveragesResponse) {
            return (IndustryAveragesResponse) cached;
        }

        String sql = "SELECT sector, industry, company_count, avg_market_cap, avg_pe_ratio, "
                + "avg_revenue_growth, avg_roe, avg_debt_to_equity, avg_net_margin, "
                + "pe_25th, pe_50th, pe_75th "
                + "FROM " + MV_INDUSTRY_AVERAGES
                + " WHERE sector = ? AND industry = ?";

        List<IndustryAveragesResponse> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> new IndustryAveragesResponse(
                        rs.getString("sector"),
                        rs.getString("industry"),
                        rs.getInt("company_count"),
                        getBigDecimal(rs, "avg_market_cap"),
                        getBigDecimal(rs, "avg_pe_ratio"),
                        getBigDecimal(rs, "avg_revenue_growth"),
                        getBigDecimal(rs, "avg_roe"),
                        getBigDecimal(rs, "avg_debt_to_equity"),
                        getBigDecimal(rs, "avg_net_margin"),
                        getBigDecimal(rs, "pe_25th"),
                        getBigDecimal(rs, "pe_50th"),
                        getBigDecimal(rs, "pe_75th")
                ),
                sector, industry);

        IndustryAveragesResponse response = results.isEmpty() ? null : results.get(0);

        if (response != null) {
            redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_HOURS, TimeUnit.HOURS);
        }

        return response;
    }

    /**
     * Get all industries.
     */
    public List<Industry> getAllIndustries() {
        return industryRepository.findAllByOrderBySectorAscIndustryAsc();
    }

    /**
     * Compute percentile rank for a company metric within its sector/industry.
     * Uses PostgreSQL PERCENT_RANK() window function.
     */
    public BigDecimal computePercentileRank(String ticker, String metric, BigDecimal value) {
        if (value == null) {
            return null;
        }

        // Validate metric whitelist to prevent SQL injection
        Set<String> allowedMetrics = Set.of(
                "market_cap", "pe_ratio", "revenue_growth_yoy", "roe",
                "debt_to_equity", "net_margin", "dividend_yield"
        );
        if (!allowedMetrics.contains(metric)) {
            return null;
        }

        String sql = "WITH ranked AS ("
                + "  SELECT ticker, " + metric + ", "
                + "    PERCENT_RANK() OVER (ORDER BY " + metric + " NULLS LAST) AS pct_rank "
                + "  FROM mv_screener_data WHERE " + metric + " IS NOT NULL"
                + ") SELECT pct_rank * 100 FROM ranked WHERE ticker = ?";

        try {
            Double result = jdbcTemplate.queryForObject(sql, Double.class, ticker.toUpperCase());
            return result != null
                    ? BigDecimal.valueOf(result).setScale(2, java.math.RoundingMode.HALF_UP)
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getBigDecimal(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        BigDecimal val = rs.getBigDecimal(column);
        return rs.wasNull() ? null : val;
    }
}

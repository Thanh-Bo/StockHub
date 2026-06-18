package com.stockhub.screener.service;

import com.stockhub.common.enums.FilterOperator;
import com.stockhub.screener.dto.FilterCriteria;
import com.stockhub.screener.dto.FilterMetadata;
import com.stockhub.screener.dto.ScreenerRequest;
import com.stockhub.screener.dto.ScreenerResponse;
import com.stockhub.screener.dto.ScreenerResultItem;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ScreenerService {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "ticker", "name", "sector", "market_cap", "pe_ratio",
            "revenue_growth_yoy", "roe", "dividend_yield", "debt_to_equity", "net_margin"
    );

    private static final String TABLE = "mv_screener_data";
    private static final String CACHE_PREFIX = "screener::results::";
    private static final long CACHE_TTL_HOURS = 1;

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public ScreenerService(JdbcTemplate jdbcTemplate,
                           RedisTemplate<String, Object> redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Execute a screener search with filters, sorting, and pagination.
     */
    public ScreenerResponse search(ScreenerRequest request) {
        // Check cache first
        String cacheKey = CACHE_PREFIX + request.hashCode();
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ScreenerResponse scr) {
            return scr;
        }

        List<Object> params = new ArrayList<>();

        // Build WHERE clauses
        StringBuilder whereClause = new StringBuilder();
        List<FilterCriteria> filters = request.filters();
        if (filters != null) {
            for (FilterCriteria filter : filters) {
                appendFilter(whereClause, params, filter);
            }
        }

        String where = whereClause.length() > 0 ? whereClause.toString() : " WHERE 1=1";

        // Validate sort
        String sortField = "market_cap";
        String sortDirection = "DESC";
        if (request.sort() != null && request.sort().field() != null) {
            if (ALLOWED_SORT.contains(request.sort().field())) {
                sortField = request.sort().field();
            }
            if (request.sort().direction() != null
                    && ("ASC".equalsIgnoreCase(request.sort().direction())
                    || "DESC".equalsIgnoreCase(request.sort().direction()))) {
                sortDirection = request.sort().direction().toUpperCase();
            }
        }

        int page = request.pagination() != null ? request.pagination().page() : 0;
        int size = request.pagination() != null ? request.pagination().size() : 25;
        int offset = page * size;

        // Data query
        String dataSql = "SELECT ticker, name, sector, industry, market_cap, pe_ratio, "
                + "revenue_growth_yoy, roe, dividend_yield, debt_to_equity, net_margin "
                + "FROM " + TABLE + where
                + " ORDER BY " + sortField + " " + sortDirection
                + " LIMIT ? OFFSET ?";

        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(size);
        dataParams.add(offset);

        List<ScreenerResultItem> content = jdbcTemplate.query(
                dataSql,
                dataParams.toArray(),
                this::mapRow
        );

        // Count query
        long totalElements = countResults(where, params);

        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        ScreenerResponse response = new ScreenerResponse(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1
        );

        // Cache result
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_HOURS, TimeUnit.HOURS);

        return response;
    }

    /**
     * Get available filter metadata.
     */
    public List<FilterMetadata> getAvailableFilters() {
        return List.of(
                new FilterMetadata("market_cap", "Market Cap", "RANGE",
                        BigDecimal.ZERO, null, null),
                new FilterMetadata("pe_ratio", "P/E Ratio", "RANGE",
                        BigDecimal.ZERO, BigDecimal.valueOf(1000), null),
                new FilterMetadata("revenue_growth_yoy", "Revenue Growth YoY", "RANGE",
                        BigDecimal.valueOf(-100), BigDecimal.valueOf(500), null),
                new FilterMetadata("roe", "ROE", "RANGE",
                        BigDecimal.valueOf(-100), BigDecimal.valueOf(200), null),
                new FilterMetadata("debt_to_equity", "Debt to Equity", "RANGE",
                        BigDecimal.ZERO, BigDecimal.valueOf(100), null),
                new FilterMetadata("net_margin", "Net Margin", "RANGE",
                        BigDecimal.valueOf(-100), BigDecimal.valueOf(100), null),
                new FilterMetadata("dividend_yield", "Dividend Yield", "RANGE",
                        BigDecimal.ZERO, BigDecimal.valueOf(50), null),
                new FilterMetadata("sector", "Sector", "SELECT",
                        null, null,
                        jdbcTemplate.queryForList(
                                "SELECT DISTINCT sector FROM " + TABLE + " WHERE sector IS NOT NULL ORDER BY sector",
                                String.class))
        );
    }

    // --- Private helpers ---

    private void appendFilter(StringBuilder where, List<Object> params, FilterCriteria filter) {
        String field = filter.field();
        FilterOperator operator = filter.operator();

        switch (operator) {
            case BETWEEN -> {
                BigDecimal min = filter.getMinValueAsBigDecimal();
                BigDecimal max = filter.getMaxValueAsBigDecimal();
                if (min != null && max != null) {
                    where.append(" AND ").append(field).append(" >= ? AND ").append(field).append(" <= ?");
                    params.add(min);
                    params.add(max);
                } else if (min != null) {
                    where.append(" AND ").append(field).append(" >= ?");
                    params.add(min);
                } else if (max != null) {
                    where.append(" AND ").append(field).append(" <= ?");
                    params.add(max);
                }
            }
            case GREATER_THAN -> {
                BigDecimal val = filter.getValueAsBigDecimal();
                if (val != null) {
                    where.append(" AND ").append(field).append(" > ?");
                    params.add(val);
                }
            }
            case GREATER_THAN_OR_EQUAL -> {
                BigDecimal val = filter.getValueAsBigDecimal();
                if (val != null) {
                    where.append(" AND ").append(field).append(" >= ?");
                    params.add(val);
                }
            }
            case LESS_THAN -> {
                BigDecimal val = filter.getValueAsBigDecimal();
                if (val != null) {
                    where.append(" AND ").append(field).append(" < ?");
                    params.add(val);
                }
            }
            case LESS_THAN_OR_EQUAL -> {
                BigDecimal val = filter.getValueAsBigDecimal();
                if (val != null) {
                    where.append(" AND ").append(field).append(" <= ?");
                    params.add(val);
                }
            }
            case EQUAL -> {
                BigDecimal val = filter.getValueAsBigDecimal();
                if (val != null) {
                    where.append(" AND ").append(field).append(" = ?");
                    params.add(val);
                }
            }
            case IN -> {
                List<String> values = filter.values();
                if (values != null && !values.isEmpty()) {
                    where.append(" AND ").append(field).append(" IN (");
                    for (int i = 0; i < values.size(); i++) {
                        if (i > 0) where.append(", ");
                        where.append("?");
                        params.add(values.get(i));
                    }
                    where.append(")");
                }
            }
        }
    }

    private long countResults(String where, List<Object> params) {
        String countSql = "SELECT COUNT(*) FROM " + TABLE + where;
        try {
            Long count = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
            return count != null ? count : 0L;
        } catch (Exception e) {
            // Fallback to reltuples estimate
            try {
                Long estimate = jdbcTemplate.queryForObject(
                        "SELECT reltuples::bigint FROM pg_class WHERE relname = '" + TABLE + "'",
                        Long.class);
                return estimate != null ? estimate : 0L;
            } catch (Exception ex) {
                return 0L;
            }
        }
    }

    private ScreenerResultItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ScreenerResultItem(
                rs.getString("ticker"),
                rs.getString("name"),
                rs.getString("sector"),
                rs.getString("industry"),
                getBigDecimal(rs, "market_cap"),
                getBigDecimal(rs, "pe_ratio"),
                getBigDecimal(rs, "revenue_growth_yoy"),
                getBigDecimal(rs, "roe"),
                getBigDecimal(rs, "dividend_yield"),
                getBigDecimal(rs, "debt_to_equity"),
                getBigDecimal(rs, "net_margin")
        );
    }

    private BigDecimal getBigDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal val = rs.getBigDecimal(column);
        return rs.wasNull() ? null : val;
    }
}

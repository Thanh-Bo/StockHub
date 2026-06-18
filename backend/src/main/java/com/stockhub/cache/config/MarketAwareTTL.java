package com.stockhub.cache.config;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Market-aware TTL calculator that adjusts cache durations based on
 * whether the US stock market is currently open.
 * <p>
 * Market hours: Monday-Friday 9:30 AM - 4:00 PM Eastern Time.
 * </p>
 */
@Component
public class MarketAwareTTL {

    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    private static final Duration MARKET_OPEN_DASHBOARD_TTL = Duration.ofHours(1);
    private static final Duration MARKET_CLOSED_DASHBOARD_TTL = Duration.ofHours(24);
    private static final Duration MARKET_OPEN_PRICE_TTL = Duration.ofMinutes(5);
    private static final Duration MARKET_CLOSED_PRICE_TTL = Duration.ofHours(24);

    /**
     * Check whether the US stock market is currently open.
     *
     * @return {@code true} if it's Monday-Friday between 9:30 AM and 4:00 PM ET
     */
    public boolean isMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(MARKET_ZONE);
        DayOfWeek day = now.getDayOfWeek();

        // Weekend check
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime time = now.toLocalTime();
        return !time.isBefore(MARKET_OPEN) && !time.isAfter(MARKET_CLOSE);
    }

    /**
     * Get the appropriate TTL for dashboard cache entries.
     * <ul>
     *   <li>During market hours: 1 hour</li>
     *   <li>Outside market hours: 24 hours</li>
     * </ul>
     */
    public Duration getDashboardTTL() {
        return isMarketOpen() ? MARKET_OPEN_DASHBOARD_TTL : MARKET_CLOSED_DASHBOARD_TTL;
    }

    /**
     * Get the appropriate TTL for stock price cache entries.
     * <ul>
     *   <li>During market hours: 5 minutes</li>
     *   <li>Outside market hours: 24 hours</li>
     * </ul>
     */
    public Duration getPriceTTL() {
        return isMarketOpen() ? MARKET_OPEN_PRICE_TTL : MARKET_CLOSED_PRICE_TTL;
    }
}

package com.stockhub.auth.security;

import com.stockhub.common.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private static final int ANONYMOUS_LIMIT = 30;
    private static final int FREE_LIMIT = 60;
    private static final int PREMIUM_LIMIT = 300;
    private static final int ADMIN_LIMIT = 1000;

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimitFilter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String endpoint = request.getRequestURI();
        String key = RATE_LIMIT_KEY_PREFIX + clientIp + ":" + endpoint;

        int limit = getLimitForRequest(request);
        long currentCount = incrementAndGet(key);
        long ttl = getTtl(key);

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - currentCount)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(Instant.now().plusSeconds(ttl).getEpochSecond()));

        if (currentCount > limit) {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {} (count: {}, limit: {})",
                    clientIp, endpoint, currentCount, limit);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(ttl));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"type\":\"https://api.stockhub.com/errors/rate-limit-exceeded\"," +
                    "\"title\":\"Too Many Requests\"," +
                    "\"status\":429," +
                    "\"detail\":\"Rate limit exceeded. Try again in " + ttl + " seconds.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private long incrementAndGet(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, RATE_LIMIT_WINDOW);
        }
        return count != null ? count : 0;
    }

    private long getTtl(String key) {
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    private int getLimitForRequest(HttpServletRequest request) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal principal) {
            String role = principal.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("ROLE_FREE");

            if (role.contains(UserRole.ADMIN.name())) {
                return ADMIN_LIMIT;
            } else if (role.contains(UserRole.PREMIUM.name())) {
                return PREMIUM_LIMIT;
            } else {
                return FREE_LIMIT;
            }
        }

        return ANONYMOUS_LIMIT;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}

package com.stockhub.cache.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration for caching and data serialization.
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate configured with JSON serialization for values
     * and String serialization for keys.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // JSON serializer with JavaTimeModule for LocalDate/Instant support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        Jackson2JsonRedisSerializer<Object> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * CacheManager with default 1-hour TTL and custom TTLs for specific cache regions.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default config: 1 hour TTL
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(createJsonSerializer()))
                .disableCachingNullValues();

        // Custom TTL configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Company profiles: cache for 24 hours
        cacheConfigurations.put("company::profile",
                defaultConfig.entryTtl(Duration.ofHours(24)));

        // Screener results: cache for 1 hour (same as default but explicit)
        cacheConfigurations.put("screener::results",
                defaultConfig.entryTtl(Duration.ofHours(1)));

        // Search autocomplete: cache for 6 hours
        cacheConfigurations.put("search::autocomplete",
                defaultConfig.entryTtl(Duration.ofHours(6)));

        // Stock prices: 5 minutes during market hours, overridden by MarketAwareTTL
        cacheConfigurations.put("prices::latest",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Comparison results: 1 hour
        cacheConfigurations.put("comparison::results",
                defaultConfig.entryTtl(Duration.ofHours(1)));

        // Dashboard data: 1 hour default, overridden by MarketAwareTTL
        cacheConfigurations.put("dashboard",
                defaultConfig.entryTtl(Duration.ofHours(1)));

        // Industry averages: 24 hours
        cacheConfigurations.put("industry::averages",
                defaultConfig.entryTtl(Duration.ofHours(24)));

        // Financial data: 6 hours
        cacheConfigurations.put("financials",
                defaultConfig.entryTtl(Duration.ofHours(6)));

        // Watchlist data: 30 minutes
        cacheConfigurations.put("watchlist",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private Jackson2JsonRedisSerializer<Object> createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }
}

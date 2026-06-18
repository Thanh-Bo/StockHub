package com.stockhub.auth.service;

import com.stockhub.auth.config.JwtProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final String TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration tokenTtl;

    public RefreshTokenService(RedisTemplate<String, Object> redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.tokenTtl = jwtProperties.refreshTokenExpiry();
    }

    public void store(String tokenId, UUID userId) {
        String tokenKey = TOKEN_PREFIX + tokenId;
        String userTokensKey = USER_TOKENS_PREFIX + userId;

        redisTemplate.opsForHash().put(tokenKey, "userId", userId.toString());
        redisTemplate.opsForHash().put(tokenKey, "createdAt", String.valueOf(System.currentTimeMillis()));
        redisTemplate.expire(tokenKey, tokenTtl);

        redisTemplate.opsForSet().add(userTokensKey, tokenId);
        redisTemplate.expire(userTokensKey, tokenTtl);
    }

    public boolean validate(String tokenId) {
        String tokenKey = TOKEN_PREFIX + tokenId;
        Boolean exists = redisTemplate.hasKey(tokenKey);
        return Boolean.TRUE.equals(exists);
    }

    public UUID getUserId(String tokenId) {
        String tokenKey = TOKEN_PREFIX + tokenId;
        Object userIdObj = redisTemplate.opsForHash().get(tokenKey, "userId");
        if (userIdObj == null) {
            return null;
        }
        return UUID.fromString(userIdObj.toString());
    }

    public void delete(String tokenId) {
        String tokenKey = TOKEN_PREFIX + tokenId;
        UUID userId = getUserId(tokenId);
        if (userId != null) {
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().remove(userTokensKey, tokenId);
        }
        redisTemplate.delete(tokenKey);
    }

    public void deleteAllForUser(UUID userId) {
        String userTokensKey = USER_TOKENS_PREFIX + userId;
        Set<Object> tokenIds = redisTemplate.opsForSet().members(userTokensKey);
        if (tokenIds != null) {
            for (Object tokenId : tokenIds) {
                String tokenKey = TOKEN_PREFIX + tokenId.toString();
                redisTemplate.delete(tokenKey);
            }
        }
        redisTemplate.delete(userTokensKey);
    }
}

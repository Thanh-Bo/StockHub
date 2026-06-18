package com.stockhub.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "stockhub.jwt")
public record JwtProperties(
        String privateKey,
        String publicKey,
        @DefaultValue("15m") Duration accessTokenExpiry,
        @DefaultValue("7d") Duration refreshTokenExpiry,
        @DefaultValue("stockhub-api") String issuer
) {}

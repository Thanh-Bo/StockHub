package com.stockhub.auth.service;

import com.stockhub.auth.config.JwtProperties;
import com.stockhub.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public JwtService(JwtProperties jwtProperties, RefreshTokenService refreshTokenService) {
        this.jwtProperties = jwtProperties;
        this.refreshTokenService = refreshTokenService;
    }

    @PostConstruct
    public void init() {
        KeyPair keyPair = loadOrGenerateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.accessTokenExpiry());

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        String tokenId = UUID.randomUUID().toString();
        refreshTokenService.store(tokenId, userId);
        return tokenId;
    }

    public Claims validateAndParseAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((PublicKey) publicKey)
                    .requireIssuer(jwtProperties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Access token expired: {}", e.getMessage());
            throw new JwtException("Access token has expired", e);
        } catch (JwtException e) {
            log.debug("Invalid access token: {}", e.getMessage());
            throw new JwtException("Invalid access token", e);
        }
    }

    public String rotateRefreshToken(String oldToken, UUID userId) {
        if (!refreshTokenService.validate(oldToken)) {
            log.warn("Refresh token reuse detected for user: {}. Revoking all tokens.", userId);
            revokeAllUserTokens(userId);
            throw new JwtException("Refresh token has been revoked or is invalid");
        }
        refreshTokenService.delete(oldToken);
        return generateRefreshToken(userId);
    }

    public void revokeAllUserTokens(UUID userId) {
        refreshTokenService.deleteAllForUser(userId);
    }

    public void revokeRefreshToken(String tokenId) {
        refreshTokenService.delete(tokenId);
    }

    public long getAccessTokenExpirySeconds() {
        return jwtProperties.accessTokenExpiry().toSeconds();
    }

    private KeyPair loadOrGenerateKeyPair() {
        String privateKeyB64 = jwtProperties.privateKey();
        String publicKeyB64 = jwtProperties.publicKey();

        if (privateKeyB64 != null && !privateKeyB64.isBlank()
                && publicKeyB64 != null && !publicKeyB64.isBlank()) {
            try {
                log.info("Loading RSA key pair from configuration properties");
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");

                byte[] privateBytes = Base64.getDecoder().decode(
                        privateKeyB64.replace("-----BEGIN PRIVATE KEY-----", "")
                                .replace("-----END PRIVATE KEY-----", "")
                                .replaceAll("\\s", ""));
                PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateBytes);
                PrivateKey privKey = keyFactory.generatePrivate(privateSpec);

                byte[] publicBytes = Base64.getDecoder().decode(
                        publicKeyB64.replace("-----BEGIN PUBLIC KEY-----", "")
                                .replace("-----END PUBLIC KEY-----", "")
                                .replaceAll("\\s", ""));
                X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicBytes);
                PublicKey pubKey = keyFactory.generatePublic(publicSpec);

                return new KeyPair(pubKey, privKey);
            } catch (Exception e) {
                log.error("Failed to load RSA key pair from properties, generating new keys", e);
            }
        }

        log.info("Generating new RSA 2048 key pair for JWT signing");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }
}

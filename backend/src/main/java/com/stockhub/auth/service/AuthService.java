package com.stockhub.auth.service;

import com.stockhub.auth.dto.LoginRequest;
import com.stockhub.auth.dto.LoginResponse;
import com.stockhub.auth.dto.RefreshRequest;
import com.stockhub.auth.dto.RegisterRequest;
import com.stockhub.auth.dto.UserResponse;
import com.stockhub.auth.entity.User;
import com.stockhub.auth.repository.UserRepository;
import com.stockhub.common.enums.AuthProvider;
import com.stockhub.common.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RedisTemplate<String, Object> redisTemplate;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.redisTemplate = redisTemplate;
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(UserRole.FREE)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(false)
                .createdAt(Instant.now())
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        return buildLoginResponse(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());
        return buildLoginResponse(user);
    }

    public LoginResponse refresh(RefreshRequest request) {
        UUID userId = refreshTokenService.getUserId(request.refreshToken());
        if (userId == null) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String newRefreshToken = jwtService.rotateRefreshToken(request.refreshToken(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accessToken = jwtService.generateAccessToken(user);
        long expiresIn = jwtService.getAccessTokenExpirySeconds();

        return new LoginResponse(
                accessToken,
                newRefreshToken,
                expiresIn,
                toUserResponse(user)
        );
    }

    public void logout(String refreshToken) {
        refreshTokenService.delete(refreshToken);
        log.debug("Refresh token revoked");
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return toUserResponse(user);
    }

    public LoginResponse handleGoogleOAuth(OAuth2User oAuth2User) {
        String googleId = oAuth2User.getName();
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        if (email == null) {
            throw new IllegalArgumentException("Google OAuth2 response missing email");
        }

        User user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .googleId(googleId)
                            .firstName(firstName != null ? firstName : "")
                            .lastName(lastName != null ? lastName : "")
                            .role(UserRole.FREE)
                            .authProvider(AuthProvider.GOOGLE)
                            .emailVerified(true)
                            .createdAt(Instant.now())
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser);
                });

        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in via Google OAuth: {}", user.getEmail());
        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        long expiresIn = jwtService.getAccessTokenExpirySeconds();

        return new LoginResponse(
                accessToken,
                refreshToken,
                expiresIn,
                toUserResponse(user)
        );
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}

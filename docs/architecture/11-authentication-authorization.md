# Section 11: Authentication & Authorization

## 11.1 Authentication Flow

### Registration (Email/Password)

```
Client                          Server
  │                               │
  │  POST /api/v1/auth/register   │
  │  {email, password, name}      │
  │──────────────────────────────>│
  │                               │ → Validate input
  │                               │ → Check email uniqueness
  │                               │ → BCrypt hash password (cost=12)
  │                               │ → Save user (role=FREE, emailVerified=false)
  │                               │ → Generate email verification token (Redis, TTL=24h)
  │  201 Created                  │
  │<──────────────────────────────│
```

### Login

```
Client                          Server
  │                               │
  │  POST /api/v1/auth/login      │
  │  {email, password}            │
  │──────────────────────────────>│
  │                               │ → Find user by email
  │                               │ → BCrypt.verify(password, hash)
  │                               │ → Generate access token (JWT, 15 min)
  │                               │ → Generate refresh token (opaque, 7 days, stored in Redis)
  │                               │ → Update lastLoginAt
  │  200 OK                       │
  │  {accessToken, refreshToken,  │
  │   expiresIn, user}            │
  │<──────────────────────────────│
```

### Token Refresh

```
Client                          Server
  │                               │
  │  POST /api/v1/auth/refresh    │
  │  {refreshToken}               │
  │──────────────────────────────>│
  │                               │ → Lookup refresh token in Redis
  │                               │ → Validate: not expired, not revoked
  │                               │ → Generate new access token
  │                               │ → Rotate refresh token (delete old, create new)
  │  200 OK                       │
  │  {accessToken, refreshToken}  │
  │<──────────────────────────────│
```

### Google OAuth2

```
Client                          Server                      Google
  │                               │                           │
  │  GET /auth/oauth2/google      │                           │
  │──────────────────────────────>│                           │
  │  302 → Google OAuth URL       │                           │
  │<──────────────────────────────│                           │
  │                                                           │
  │  User authenticates with Google                           │
  │──────────────────────────────────────────────────────────>│
  │  Google redirects with code   │                           │
  │──────────────────────────────>│                           │
  │                               │ → Exchange code for token │
  │                               │──────────────────────────>│
  │                               │ ← id_token, user info     │
  │                               │<──────────────────────────│
  │                               │ → Find or create user     │
  │                               │   (by googleId or email)  │
  │                               │ → Generate JWT            │
  │  302 → /dashboard?token=...   │                           │
  │<──────────────────────────────│                           │
```

---

## 11.2 JWT Design

### Access Token (JWT)

```json
{
  "iss": "stockhub-api",
  "sub": "user-uuid-here",
  "email": "user@example.com",
  "role": "FREE",
  "iat": 1687459200,
  "exp": 1687460100
}
```

- **Algorithm**: RS256 (asymmetric — private key on server, public key for verification)
- **Expiry**: 15 minutes
- **Stored**: Client memory only (never localStorage)
- **Transmitted**: `Authorization: Bearer {token}` header

### Refresh Token (Opaque)

- **Format**: Random UUID, stored in Redis
- **Expiry**: 7 days
- **Rotation**: Each refresh issues a new refresh token and invalidates the old one
- **Reuse detection**: If a revoked refresh token is used → revoke all tokens for that user (stolen token detection)

---

## 11.3 Security Configuration

```java
package com.stockhub.auth;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)  // JWT = stateless, CSRF not needed
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(GET, "/api/v1/companies/**").permitAll()
                .requestMatchers(GET, "/api/v1/search/**").permitAll()
                .requestMatchers(POST, "/api/v1/screener/search").permitAll()
                .requestMatchers(POST, "/api/v1/companies/compare").permitAll()
                .requestMatchers(GET, "/api/v1/industries/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Authenticated endpoints
                .requestMatchers("/api/v1/watchlists/**").authenticated()
                .requestMatchers("/api/v1/exports/**").authenticated()

                // Premium-only endpoints
                .requestMatchers(POST, "/api/v1/screener/saved").hasRole("PREMIUM")
                .requestMatchers("/api/v1/exports/**").hasRole("PREMIUM")

                // Admin-only endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(ae -> ae
                    .baseUri("/api/v1/auth/oauth2/google"))
                .successHandler(oAuth2SuccessHandler())
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // 12 rounds ≈ 250ms on modern CPU
    }
}
```

---

## 11.4 JWT Authentication Filter

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims = jwtService.validateAndParse(token);
            UserPrincipal principal = new UserPrincipal(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("role", String.class)
            );

            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
```

---

## 11.5 Role-Based Authorization

### Roles

| Role | Permissions |
|------|------------|
| `FREE` | Dashboard, search, basic screener (5 filters), 1 watchlist (max 10 stocks), price charts |
| `PREMIUM` | FREE + advanced screener (all filters), unlimited watchlists, exports (Excel/PDF), industry comparison, saved screeners |
| `ADMIN` | PREMIUM + user management, ETL controls, system monitoring |

### Method-Level Security

```java
@RestController
@RequestMapping("/api/v1/watchlists")
public class WatchlistController {

    @PostMapping
    @PreAuthorize("hasRole('FREE')")
    public ResponseEntity<WatchlistResponse> createWatchlist(
            @Valid @RequestBody CreateWatchlistRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        // ...
    }

    @PostMapping("/{id}/stocks")
    @PreAuthorize("hasRole('FREE')")
    public ResponseEntity<Void> addStock(
            @PathVariable UUID id,
            @Valid @RequestBody AddStockRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        // Check: FREE users max 10 stocks per watchlist
        // Check: Watchlist belongs to authenticated user
        // ...
    }
}
```

### Ownership Validation (beyond role check)

```java
// Custom annotation for resource ownership
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireOwnership {
    String resourceIdParam();
}

// Aspect that verifies the authenticated user owns the resource
@Aspect
@Component
public class OwnershipAspect {

    @Around("@annotation(requireOwnership)")
    public Object checkOwnership(ProceedingJoinPoint pjp, RequireOwnership requireOwnership) {
        UserPrincipal user = (UserPrincipal) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        UUID resourceId = extractResourceId(pjp, requireOwnership.resourceIdParam());

        if (!resourceBelongsToUser(resourceId, user.getId())) {
            throw new AccessDeniedException("Resource does not belong to user");
        }

        return pjp.proceed();
    }
}
```

---

## 11.6 Security Best Practices

| Practice | Implementation |
|----------|---------------|
| **Password hashing** | BCrypt with cost factor 12 |
| **JWT signing** | RS256 (asymmetric) — private key never leaves server |
| **Token storage** | Access token in memory (Angular service variable), refresh token in httpOnly cookie |
| **Refresh token rotation** | New refresh token on each use; old one invalidated |
| **Reuse detection** | If revoked refresh token is presented → revoke all user tokens (stolen token response) |
| **Rate limiting auth** | Stricter limits: 5 login attempts/minute per IP |
| **CORS** | Whitelist only `https://stockhub.com` and `http://localhost:4200` |
| **HTTPS only** | HSTS header, all cookies `Secure; HttpOnly; SameSite=Strict` |
| **SQL injection** | JPA parameterized queries + input validation |
| **XSS** | Angular auto-sanitizes; CSP headers configured |
| **Secrets** | Never in code; use environment variables / Azure Key Vault |
| **Audit logging** | Log all login attempts (success + failure), role changes |

---

## 11.7 Redis Token Storage

| Key Pattern | Value | TTL |
|-------------|-------|-----|
| `refresh_token:{tokenId}` | `{"userId": "uuid", "createdAt": "..."}` | 7 days |
| `user_tokens:{userId}` | Set of active refresh token IDs | 7 days |
| `blacklist:{tokenId}` | `"revoked"` | 7 days (remaining TTL) |
| `email_verify:{token}` | `{"userId": "uuid"}` | 24 hours |
| `login_attempts:{ip}` | Integer counter | 5 minutes |

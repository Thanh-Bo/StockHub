package com.stockhub.auth.config;

import com.stockhub.auth.security.JwtAuthFilter;
import com.stockhub.auth.security.RateLimitFilter;
import com.stockhub.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final AuthService authService;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          RateLimitFilter rateLimitFilter,
                          AuthService authService,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.authService = authService;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/companies/**",
                                "/api/v1/search/**",
                                "/api/v1/industries/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/screener/**",
                                "/api/v1/compare/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // PREMIUM role required for saved screeners and exports
                        .requestMatchers("/api/v1/screener/saved/**").hasRole("PREMIUM")
                        .requestMatchers("/api/v1/exports/**").hasRole("PREMIUM")
                        // ADMIN role required for admin endpoints
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // Watchlist and exports require authentication
                        .requestMatchers("/api/v1/watchlists/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {
                            OAuth2User oAuth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
                            var loginResponse = authService.handleGoogleOAuth(oAuth2User);

                            // For API clients, return JSON instead of redirect
                            String acceptHeader = request.getHeader("Accept");
                            if (acceptHeader != null && acceptHeader.contains("application/json")) {
                                response.setContentType("application/json");
                                response.setStatus(HttpServletResponse.SC_OK);
                                response.getWriter().write(
                                        "{\"accessToken\":\"" + loginResponse.accessToken() + "\"," +
                                                "\"refreshToken\":\"" + loginResponse.refreshToken() + "\"," +
                                                "\"expiresIn\":" + loginResponse.expiresIn() + "," +
                                                "\"user\":{\"id\":\"" + loginResponse.user().id() + "\"," +
                                                "\"email\":\"" + loginResponse.user().email() + "\"," +
                                                "\"firstName\":\"" + loginResponse.user().firstName() + "\"," +
                                                "\"lastName\":\"" + loginResponse.user().lastName() + "\"," +
                                                "\"role\":\"" + loginResponse.user().role() + "\"," +
                                                "\"emailVerified\":" + loginResponse.user().emailVerified() + "," +
                                                "\"createdAt\":\"" + loginResponse.user().createdAt() + "\"}}"
                                );
                            } else {
                                response.sendRedirect("/login/oauth2/success");
                            }
                        })
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write(
                                    "{\"type\":\"https://api.stockhub.com/errors/unauthorized\"," +
                                            "\"title\":\"Authentication Required\"," +
                                            "\"status\":401," +
                                            "\"detail\":\"Full authentication is required to access this resource\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write(
                                    "{\"type\":\"https://api.stockhub.com/errors/forbidden\"," +
                                            "\"title\":\"Access Denied\"," +
                                            "\"status\":403," +
                                            "\"detail\":\"You do not have permission to access this resource\"}"
                            );
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}

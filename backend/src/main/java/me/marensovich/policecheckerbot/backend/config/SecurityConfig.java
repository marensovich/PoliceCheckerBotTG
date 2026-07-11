package me.marensovich.policecheckerbot.backend.config;

import lombok.RequiredArgsConstructor;
import me.marensovich.policecheckerbot.backend.security.SessionFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for DPS Tracker.
 *
 * <p>Authentication is stateless, using a UUID session token stored in the database
 * and supplied by the client as {@code Authorization: Bearer <token>}. There is no JWT —
 * the initial token is obtained by posting a signed Telegram WebApp {@code initData} to
 * {@code POST /api/auth/telegram}.
 *
 * <p>Public endpoints:
 * <ul>
 *   <li>{@code POST /api/auth/telegram} — authentication entry point</li>
 *   <li>{@code GET /api/posts} — map view without login</li>
 *   <li>{@code /ws/**} — WebSocket (STOMP over SockJS)</li>
 *   <li>{@code /error} — Spring error page</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionFilter sessionFilter;

    /**
     * Build the security filter chain.
     *
     * <p>CSRF is disabled (stateless API + CORS handles origin validation).
     * The {@link SessionFilter} is inserted before
     * {@link UsernamePasswordAuthenticationFilter} to populate the
     * {@link org.springframework.security.core.context.SecurityContextHolder}.
     *
     * @param http the {@link HttpSecurity} builder
     * @return configured {@link SecurityFilterChain}
     * @throws Exception if the configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/telegram").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    /**
     * CORS configuration permitting all origins with credentials.
     *
     * <p>Allows requests from the Telegram Mini App (hosted on a Telegram CDN domain)
     * and the local development server.
     *
     * @return CORS configuration source applied to all paths
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

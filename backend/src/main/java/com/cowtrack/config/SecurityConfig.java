package com.cowtrack.config;

import com.cowtrack.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** Origins allowed to call the API, from {@code cowtrack.cors.allowed-origins}. */
    @Value("${cowtrack.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS must be enabled on the filter chain itself. A @CrossOrigin
                // annotation alone is not enough: preflight OPTIONS requests carry no
                // credentials and would be rejected here before ever reaching a controller.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                // JWTs carry the session, so no server-side session is needed.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/health", "/api/ping", "/api/info").permitAll()
                        // Collars have no interactive user and cannot hold a JWT.
                        // Exempt from the token chain, but NOT unauthenticated:
                        // DeviceAuthenticator checks the key inside the service and
                        // rejects with 401 exactly as the filter would.
                        .requestMatchers("/api/ingest/**").permitAll()
                        // EventSource cannot send an Authorization header, so the
                        // stream authenticates by redeeming a short-lived ticket
                        // inside the controller. Only the stream itself: issuing a
                        // ticket is an ordinary authenticated request, and must
                        // stay one or the ticket would be worth nothing.
                        .requestMatchers(HttpMethod.GET, "/api/realtime/stream").permitAll()
                        // Readiness/liveness probes must be reachable while auth is
                        // down or the orchestrator would kill an already-unhealthy box.
                        .requestMatchers("/actuator/health/**").permitAll()
                        // Everything else under /api needs a token.
                        .requestMatchers("/api/**").authenticated()
                        // The bundled React app is public: the browser has to be able
                        // to load the shell in order to render the login page at all.
                        // Authorisation happens on the API calls the app then makes.
                        .anyRequest().permitAll()
                )
                // Without this, an unauthenticated request gets 403. The web client's
                // axios interceptor only clears the stored token and returns to the
                // login page on 401, so an expired session would otherwise fail silently.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"success\":false,\"message\":\"Authentication required\"}");
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

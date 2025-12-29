package org.tedtalk.api.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for Spring WebFlux with authentication and authorization.
 * Integrates with CORS configuration for proper cross-origin request handling.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // Disable CSRF for API
                .cors(Customizer.withDefaults()) // Enable CORS with configuration from CorsConfig
                .authorizeExchange(authz -> authz
                        .pathMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll() // Allow CORS preflight
                        .pathMatchers(
                                "/",
                                "/actuator/**",
                                "/swagger-ui.html",
                                "/swagger-ui",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/swagger-resources/**",
                                "/swagger-resources",
                                "/favicon.ico",
                                "/error",
                                "/h2-console/**",
                                "/docs"
                        ).permitAll() // Allow public access to documentation and health endpoints
                        .anyExchange().authenticated() // Require authentication for all other requests
                )
                .httpBasic(Customizer.withDefaults()) // Use HTTP Basic authentication
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable) // Disable form login to prevent redirects
                .build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        // In production, use a proper user store like database or LDAP
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN", "USER")
                .build();

        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder().encode("user123"))
                .roles("USER")
                .build();

        return new MapReactiveUserDetailsService(admin, user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}


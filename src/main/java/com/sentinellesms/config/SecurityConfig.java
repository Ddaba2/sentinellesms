package com.sentinellesms.config;

import com.sentinellesms.security.JwtAuthenticationFilter;
import com.sentinellesms.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Mobile publics (analyse locale assistée + sync)
                        .requestMatchers(HttpMethod.POST, "/api/reports").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/analyze").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/links/check").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/phones/lookup").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/patterns/sync").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/model/latest").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/content/public").permitAll()

                        // Lecture back-office
                        .requestMatchers(HttpMethod.GET, "/api/reports/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR", "ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/statistics/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR", "ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/patterns", "/api/patterns/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR", "ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/model/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR", "ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/phones", "/api/phones/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR", "ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/links", "/api/links/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR", "ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/content", "/api/content/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR", "ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/{id}")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR", "ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/audit/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN")

                        // Modération
                        .requestMatchers(HttpMethod.POST, "/api/reports/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/phones/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/links", "/api/links/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/links", "/api/links/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/links/**")
                            .hasAnyRole("SUPER_ADMIN", "ADMIN", "MODERATOR")

                        // Administration
                        .requestMatchers("/api/patterns/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers("/api/model/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers("/api/content/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/status").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/roles").hasRole("SUPER_ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}

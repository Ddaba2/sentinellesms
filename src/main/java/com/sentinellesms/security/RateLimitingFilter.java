package com.sentinellesms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sentinellesms.dto.common.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite le nombre de requêtes par IP sur les endpoints publics (non authentifiés),
 * qui sont sinon ouverts à n'importe qui : inscription/connexion, signalement,
 * synchronisation des motifs/modèle.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private record Route(String method, String path) {
    }

    private static final Set<Route> PROTECTED_ROUTES = Set.of(
            new Route("POST", "/api/auth/register"),
            new Route("POST", "/api/auth/login"),
            new Route("POST", "/api/auth/refresh"),
            new Route("POST", "/api/reports"),
            new Route("GET", "/api/patterns/sync"),
            new Route("GET", "/api/model/latest")
    );

    private final int capacity;
    private final long windowMillis;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitingFilter(@Value("${sentinellesms.ratelimit.capacity}") int capacity,
                               @Value("${sentinellesms.ratelimit.window-seconds}") long windowSeconds) {
        this.capacity = capacity;
        this.windowMillis = windowSeconds * 1000;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        Route route = new Route(request.getMethod(), request.getRequestURI());

        if (!PROTECTED_ROUTES.contains(route) || !isRateLimited(clientKey(request, route))) {
            filterChain.doFilter(request, response);
            return;
        }

        respondTooManyRequests(response, request);
    }

    private boolean isRateLimited(String key) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(key, k -> new Window(now));

        synchronized (window) {
            if (now - window.start >= windowMillis) {
                window.start = now;
                window.count = 0;
            }
            window.count++;
            return window.count > capacity;
        }
    }

    private String clientKey(HttpServletRequest request, Route route) {
        return clientIp(request) + ":" + route.method() + ":" + route.path();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void respondTooManyRequests(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(windowMillis / 1000));

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message("Trop de requêtes, réessayez plus tard")
                .path(request.getRequestURI())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private static final class Window {
        private long start;
        private int count;

        private Window(long start) {
            this.start = start;
        }
    }
}

package com.applyflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${application.rate-limit.requests-per-minute}")
    private int requestsPerMinute;

    @Value("${application.rate-limit.auth-requests-per-minute}")
    private int authRequestsPerMinute;

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        boolean isAuthEndpoint = request.getRequestURI().startsWith("/api/auth");
        int limit = isAuthEndpoint ? authRequestsPerMinute : requestsPerMinute;

        String bucketKey = clientIp + (isAuthEndpoint ? ":auth" : ":general");
        RateLimitBucket bucket = buckets.compute(bucketKey, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new RateLimitBucket(limit);
            }
            return existing;
        });

        if (!bucket.tryConsume()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Rate limit exceeded. Please try again later.",
                    "path", request.getRequestURI())));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitBucket {
        private final AtomicInteger tokens;
        private final long expiresAt;

        RateLimitBucket(int maxTokens) {
            this.tokens = new AtomicInteger(maxTokens);
            this.expiresAt = System.currentTimeMillis() + 60_000;
        }

        boolean tryConsume() {
            return tokens.getAndDecrement() > 0;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}

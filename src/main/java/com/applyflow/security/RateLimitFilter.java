package com.applyflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final long WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${application.rate-limit.requests-per-minute}")
    private int requestsPerMinute;

    @Value("${application.rate-limit.auth-requests-per-minute}")
    private int authRequestsPerMinute;

    public RateLimitFilter(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        boolean isAuthEndpoint = request.getRequestURI().startsWith("/api/auth");
        int limit = isAuthEndpoint ? authRequestsPerMinute : requestsPerMinute;
        String bucketKey = RATE_LIMIT_PREFIX + clientIp + (isAuthEndpoint ? ":auth" : ":general");

        if (isRateLimited(bucketKey, limit, response, request)) {
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String key, int limit,
            HttpServletResponse response,
            HttpServletRequest request) throws IOException {
        if (redisTemplate == null) {
            return false; // fail-open: no Redis available
        }
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount == null) {
                log.warn("Redis INCR returned null for key: {}", key);
                return false; // fail-open
            }

            if (currentCount == 1L) {
                redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
            }

            if (currentCount > limit) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                long retryAfter = (ttl != null && ttl > 0) ? ttl : WINDOW_SECONDS;

                writeRateLimitResponse(response, request, retryAfter);
                log.warn("Rate limit exceeded for key: {} (count: {}, limit: {})", key, currentCount, limit);
                return true;
            }

            addRateLimitHeaders(response, limit, (int) (limit - currentCount));
            return false;

        } catch (Exception e) {
            log.error("Redis error during rate limiting, allowing request (fail-open): {}", e.getMessage());
            return false; // fail-open: allow request if Redis is down
        }
    }

    private void writeRateLimitResponse(HttpServletResponse response,
            HttpServletRequest request,
            long retryAfter) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfter));

        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 429,
                "error", "Too Many Requests",
                "message", "Rate limit exceeded. Please try again after " + retryAfter + " seconds.",
                "path", request.getRequestURI(),
                "retryAfter", retryAfter)));
    }

    private void addRateLimitHeaders(HttpServletResponse response, int limit, int remaining) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining)));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

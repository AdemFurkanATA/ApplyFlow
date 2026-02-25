package com.applyflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter(redisTemplate);
        ReflectionTestUtils.setField(rateLimitFilter, "requestsPerMinute", 100);
        ReflectionTestUtils.setField(rateLimitFilter, "authRequestsPerMinute", 5);
    }

    @Test
    @DisplayName("Should allow request within rate limit")
    void allowRequestWithinLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/jobs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("100");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("99");
        verify(redisTemplate).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should reject request when rate limit exceeded")
    void rejectRequestWhenLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/jobs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(101L);
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(45L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("45");
        assertThat(response.getContentAsString()).contains("Too Many Requests");
    }

    @Test
    @DisplayName("Should use stricter limit for auth endpoints")
    void useStricterLimitForAuth() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(6L);
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(30L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
    }

    @Test
    @DisplayName("Should fail-open when Redis is unavailable")
    void failOpenWhenRedisDown() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/jobs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection refused"));

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should use X-Forwarded-For header for client IP")
    void useXForwardedForHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/jobs");
        request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(contains("203.0.113.50"))).thenReturn(1L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(valueOperations).increment(contains("203.0.113.50"));
    }
}

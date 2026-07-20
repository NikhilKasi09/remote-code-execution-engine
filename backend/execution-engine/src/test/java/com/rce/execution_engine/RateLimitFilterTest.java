package com.rce.execution_engine;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private RateLimiterService rateLimiterService;
    private RateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimiterService = mock(RateLimiterService.class);
        filter = new RateLimitFilter(rateLimiterService);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void passesRequestThroughWhenUnderTheLimit() throws Exception {
        when(rateLimiterService.tryAcquire("127.0.0.1")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/execute");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200); // MockHttpServletResponse default
    }

    @Test
    void blocksWithA429WhenOverTheLimit() throws Exception {
        when(rateLimiterService.tryAcquire("127.0.0.1")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/execute");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Rate limit exceeded");
    }

    @Test
    void doesNotRateLimitUnrelatedEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).tryAcquire(org.mockito.ArgumentMatchers.anyString());
    }
}

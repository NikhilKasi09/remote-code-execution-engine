package com.rce.execution_engine;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/api/execute")) {
            String clientId = request.getRemoteAddr();

            if (!rateLimiterService.tryAcquire(clientId)) {
                response.setStatus(429); // 429 Too Many Requests - not in HttpServletResponse's SC_ constants
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please wait before submitting more code.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

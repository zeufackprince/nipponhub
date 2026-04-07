package com.nipponhub.nipponhubv0.Configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60000; // 1 minute
    
    private final ConcurrentHashMap<String, RequestCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Only rate limit auth endpoints
        if (!path.startsWith("/auth")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String clientIp = getClientIp(request);
        RequestCounter counter = counters.computeIfAbsent(clientIp, 
            k -> new RequestCounter());
        
        if (!counter.allowRequest()) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Try again in 1 minute.\"}");
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
    
    private static class RequestCounter {
        // private static final long serialVersionUID = 1L;
        private long firstRequestTime;
        private AtomicInteger count = new AtomicInteger(0);
        
        synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();
            
            if (firstRequestTime == 0 || (now - firstRequestTime) > WINDOW_MS) {
                firstRequestTime = now;
                count.set(1);
                return true;
            }
            
            if (count.incrementAndGet() <= MAX_REQUESTS) {
                return true;
            }
            return false;
        }
    }
}


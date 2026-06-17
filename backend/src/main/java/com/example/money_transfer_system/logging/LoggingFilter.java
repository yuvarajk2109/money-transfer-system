package com.example.money_transfer_system.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    private static final AtomicLong traceCounter = new AtomicLong(1);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String traceId = String.valueOf(traceCounter.getAndIncrement());
        
        MDC.put("traceId", traceId);
        MDC.put("method", request.getMethod());
        MDC.put("endpoint", request.getRequestURI());
        
        long startTime = System.currentTimeMillis();
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("duration", duration + "ms");
            // The response completion log (if needed) would go here or handled by interceptors.
            // MDC properties are automatically cleared if we clear them, but we should clear them 
            // after the request is completely processed to avoid leaking to other threads.
            MDC.clear();
        }
    }
}

package com.northstar.crm.platform.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final Logger logger = LoggerFactory.getLogger(CorrelationFilter.class);
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]{1,60}");

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = getOrCreateCorrelationId(request);
        String previousCorrelationId = MDC.get(MDC_KEY);
        long startedAt = System.nanoTime();

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;

            logger.info(
                "HTTP {} {} completed status={} durationMs={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMs
            );

            if (previousCorrelationId == null) {
                MDC.remove(MDC_KEY);
            } else {
                MDC.put(MDC_KEY, previousCorrelationId);
            }
        }
    }

    private String getOrCreateCorrelationId(HttpServletRequest request) {
        String suppliedId = request.getHeader(HEADER_NAME);

        if (suppliedId != null && VALID_ID.matcher(suppliedId).matches()) {
            return suppliedId;
        }

        return UUID.randomUUID().toString();
    }
}
package com.proveedores.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestTracingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Logger log = LoggerFactory.getLogger(RequestTracingFilter.class);
    private static final String REQUEST_ID = "requestId";
    private static final String USER = "user";
    private static final String HTTP_METHOD = "httpMethod";
    private static final String ENDPOINT = "endpoint";
    private static final String HTTP_STATUS = "httpStatus";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String requestId = resolveRequestId(request);
        MDC.put(REQUEST_ID, requestId);
        MDC.put(USER, resolveUser());
        MDC.put(HTTP_METHOD, request.getMethod());
        MDC.put(ENDPOINT, request.getRequestURI());
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            log.info("event=request.started");
            filterChain.doFilter(request, response);
        } finally {
            MDC.put(HTTP_STATUS, String.valueOf(response.getStatus()));
            log.info("event=request.completed durationMs={}", System.currentTimeMillis() - startTime);
            MDC.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    private String resolveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        return authentication.getName();
    }
}

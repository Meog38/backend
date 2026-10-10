package br.com.brainvest.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final long SLOW_REQUEST_MS = 2_000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            String path = request.getRequestURI();
            if (!path.startsWith("/actuator")) {
                long durationMs = (System.nanoTime() - started) / 1_000_000;
                String clientIp = clientIp(request);
                if (response.getStatus() >= 500 || durationMs >= SLOW_REQUEST_MS) {
                    logger.warn("http_request method={} path={} status={} duration_ms={} client_ip={}",
                            request.getMethod(), path, response.getStatus(), durationMs, clientIp);
                } else {
                    logger.info("http_request method={} path={} status={} duration_ms={} client_ip={}",
                            request.getMethod(), path, response.getStatus(), durationMs, clientIp);
                }
            }
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

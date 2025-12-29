package com.company.bank_system.security;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.RateLimiter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.lang.reflect.InaccessibleObjectException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIP = getClientIP(request);

        log.debug("RATE_LIMIT_CHECK ip={} uri={}", clientIP, request.getRequestURI());

        Bucket bucket = resolveBucket(clientIP);

        if (bucket.tryConsume(1)) {
            log.debug("RATE_LIMIT_ALLOWED ip={}", clientIP);
            filterChain.doFilter(request, response);
        } else {
            log.warn("RATE_LIMIT_EXCEEDED ip={} uri={}", clientIP, request.getRequestURI());

            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"Too many requests\", \"message\": \"Please try again later\"}"
            );
        }
    }

    private Bucket resolveBucket(String ip) {
        return buckets.computeIfAbsent(ip, k -> createNewBucket());
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
                50,  //max
                Refill.intervally(50, Duration.ofMinutes(1)) //x tokens per min
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }


    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
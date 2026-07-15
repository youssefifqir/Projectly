package com.example.Projectly.config.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH    = "/api/v1/auth/login";
    private static final String REGISTER_PATH = "/api/v1/auth/register";

    @Value("${app.rate-limit.trusted-proxies:}")
    private String trustedProxiesConfig;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    private Set<String> trustedProxies = Collections.emptySet();

    @jakarta.annotation.PostConstruct
    private void initTrustedProxies() {
        if (trustedProxiesConfig != null && !trustedProxiesConfig.isBlank()) {
            Set<String> set = new HashSet<>();
            for (String ip : trustedProxiesConfig.split(",")) {
                set.add(ip.trim());
            }
            this.trustedProxies = Set.copyOf(set);
            log.info("Trusted proxy IPs configured: {}", trustedProxies);
        }
    }

    // Per-IP bucket caches
    private final Map<String, Bucket> loginBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> businessBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain) throws ServletException, IOException {

        final String path = request.getServletPath();

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        if (LOGIN_PATH.equals(path) && "POST".equalsIgnoreCase(request.getMethod())) {
            final Bucket bucket = loginBuckets.computeIfAbsent(resolveIp(request), k -> buildLoginBucket());
            if (!bucket.tryConsume(1)) {
                rejectRequest(response, "Too many login attempts. Please try again in a minute.");
                return;
            }
        } else if (REGISTER_PATH.equals(path) && "POST".equalsIgnoreCase(request.getMethod())) {
            final Bucket bucket = registerBuckets.computeIfAbsent(resolveIp(request), k -> buildRegisterBucket());
            if (!bucket.tryConsume(1)) {
                rejectRequest(response, "Too many registration attempts. Please try again later.");
                return;
            }
        } else if (path.startsWith("/api/v1/") && !path.startsWith("/api/v1/auth/")
                && ("POST".equalsIgnoreCase(request.getMethod())
                    || "PUT".equalsIgnoreCase(request.getMethod())
                    || "DELETE".equalsIgnoreCase(request.getMethod()))) {
            final Bucket bucket = businessBuckets.computeIfAbsent(resolveIp(request), k -> buildBusinessBucket());
            if (!bucket.tryConsume(1)) {
                rejectRequest(response, "Too many requests. Please try again in a minute.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket buildLoginBucket() {
        // 5 requests per 1 minute per IP
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(5).refillIntervally(5, Duration.ofMinutes(1)).build())
                .build();
    }

    private Bucket buildRegisterBucket() {
        // 3 requests per 10 minutes per IP
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(3).refillIntervally(3, Duration.ofMinutes(10)).build())
                .build();
    }

    private Bucket buildBusinessBucket() {
        // 30 requests per 1 minute per IP for business endpoints
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(30).refillIntervally(30, Duration.ofMinutes(1)).build())
                .build();
    }

    private String resolveIp(final HttpServletRequest request) {
        final String remoteAddr = request.getRemoteAddr();
        if (trustedProxies.contains(remoteAddr)) {
            final String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    private void rejectRequest(final HttpServletResponse response, final String message) throws IOException {
        log.warn("Rate limit exceeded: {}", message);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        response.getWriter().write(
                "{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"" + message + "\"}"
        );
    }
}

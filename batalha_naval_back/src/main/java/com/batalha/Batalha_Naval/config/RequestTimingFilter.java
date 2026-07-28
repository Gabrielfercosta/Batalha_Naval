package com.batalha.Batalha_Naval.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RequestTimingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);
    private static final long SLOW_THRESHOLD_MS = 500;

    private final Timer requestTimer;
    private final Counter slowRequestCounter;

    public RequestTimingFilter(MeterRegistry registry) {
        this.requestTimer = Timer.builder("http.server.requests.custom")
                .description("Tempo das requisições HTTP")
                .register(registry);
        this.slowRequestCounter = Counter.builder("http.server.requests.slow")
                .description("Requisições lentas (>500ms)")
                .register(registry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

            requestTimer.record(Duration.ofMillis(durationMs));

            if (durationMs > SLOW_THRESHOLD_MS) {
                slowRequestCounter.increment();
                log.warn("REQUISIÇÃO LENTA: {} {} levou {}ms (status={})",
                        request.getMethod(), request.getRequestURI(), durationMs, response.getStatus());
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}

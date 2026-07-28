package com.batalha.Batalha_Naval.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RequestTimingFilterTest {

    private RequestTimingFilter filter;
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        filter = new RequestTimingFilter(registry);
    }

    @Test
    void registraMetricaDeTempoParaCadaRequisicao() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/game/open");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        Timer timer = registry.find("http.server.requests.custom").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void incrementaContadorDeRequisicoesLentas() throws ServletException, IOException {
        // Simula uma requisição lenta fazendo o filterChain demorar
        FilterChain slowChain = (req, res) -> {
            try { Thread.sleep(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        };

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/game/open");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, slowChain);

        Counter slowCounter = registry.find("http.server.requests.slow").counter();
        assertNotNull(slowCounter);
        assertEquals(1.0, slowCounter.count());
    }

    @Test
    void naoIncrementaContadorParaRequisicaoRapida() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/game/open");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        Counter slowCounter = registry.find("http.server.requests.slow").counter();
        assertNotNull(slowCounter);
        assertEquals(0.0, slowCounter.count());
    }

    @Test
    void naoFiltraActuator() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void filtraEndpointsNormais() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/game/open");
        assertFalse(filter.shouldNotFilter(request));
    }
}

package com.batalha.Batalha_Naval.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    @Test
    void permitePrimeirasRequisicoes() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/game/open");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNotEquals(429, response.getStatus());
        assertNotNull(response.getHeader("X-Rate-Limit-Remaining"));
    }

    @Test
    void bloqueiaAposLimite() throws ServletException, IOException {
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/game/open");
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
        }

        // Requisição 101 deve ser bloqueada
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/game/open");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("Limite"));
    }

    @Test
    void ipsDistintosTemBucketsIndependentes() throws ServletException, IOException {
        // Esgota limite do IP 1
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
            request.setRemoteAddr("1.1.1.1");
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        // IP 2 ainda deve funcionar
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setRemoteAddr("2.2.2.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertNotEquals(429, response.getStatus());
    }

    @Test
    void naoFiltraWebSocket() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws/something");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void filtraEndpointsApi() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/game/open");
        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void usaXForwardedForQuandoDisponivel() throws ServletException, IOException {
        // Esgota limite do IP via X-Forwarded-For
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
            request.setRemoteAddr("127.0.0.1");
            request.addHeader("X-Forwarded-For", "99.99.99.99");
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        // Mesmo remoteAddr mas sem X-Forwarded-For deve funcionar (bucket diferente)
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertNotEquals(429, response.getStatus());
    }
}

package com.batalha.Batalha_Naval.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * O rate limiting precisa barrar abuso sem atrapalhar o jogo.
 *
 * Regressão coberta: com um limite único de 100 req/min para todas as rotas, o
 * polling do lobby (3 endpoints a cada 5s por jogador) esgotava os tokens quando
 * dois jogadores dividiam o mesmo IP. As chamadas seguintes do jogo recebiam 429
 * e a tela de batalha ficava presa esperando o adversário.
 */
class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(new SimpleMeterRegistry());
    }

    private int chamar(String metodo, String uri, String ip) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void permitePollingDeDoisJogadoresNoMesmoIpPorVariosMinutos() throws Exception {
        // 2 jogadores x 3 endpoints x 12 ciclos por minuto x 3 minutos = 216 leituras
        int bloqueadas = 0;
        for (int ciclo = 0; ciclo < 36; ciclo++) {
            for (int jogador = 0; jogador < 2; jogador++) {
                for (String rota : new String[]{"/api/game/open", "/api/quiz/open", "/api/minado/open"}) {
                    if (chamar("GET", rota, "192.168.0.10") == 429) bloqueadas++;
                }
            }
        }
        assertEquals(0, bloqueadas,
                "o polling normal do lobby nao deveria ser bloqueado");
    }

    @Test
    void jogoContinuaFuncionandoDepoisDoPollingDoLobby() throws Exception {
        String ip = "192.168.0.20";
        for (int i = 0; i < 200; i++) {
            chamar("GET", "/api/game/open", ip);
        }

        // Sequência real de uma partida: criar, entrar, posicionar 5, pronto
        assertNotEquals(429, chamar("POST", "/api/game/create", ip), "criar partida foi bloqueado");
        assertNotEquals(429, chamar("POST", "/api/game/abc/join", ip), "entrar na partida foi bloqueado");
        for (int i = 0; i < 5; i++) {
            assertNotEquals(429, chamar("POST", "/api/game/abc/posicionar", ip),
                    "posicionar navio foi bloqueado");
        }
        assertNotEquals(429, chamar("POST", "/api/game/abc/pronto", ip), "marcar pronto foi bloqueado");
        // Esta e a chamada que a tela de batalha faz para descobrir o turno
        assertNotEquals(429, chamar("GET", "/api/game/abc", ip), "buscar a partida foi bloqueado");
    }

    @Test
    void bloqueiaForcaBrutaEmAutenticacao() throws Exception {
        String ip = "10.0.0.99";
        int bloqueadas = 0;
        for (int i = 0; i < 40; i++) {
            if (chamar("POST", "/api/auth/login", ip) == 429) bloqueadas++;
        }
        assertTrue(bloqueadas > 0, "tentativas repetidas de login deveriam ser barradas");
    }

    @Test
    void limiteDeAutenticacaoNaoAfetaOJogo() throws Exception {
        String ip = "10.0.0.77";
        for (int i = 0; i < 40; i++) {
            chamar("POST", "/api/auth/login", ip);
        }
        assertNotEquals(429, chamar("GET", "/api/game/open", ip),
                "esgotar o limite de login nao deveria impedir de jogar");
        assertNotEquals(429, chamar("POST", "/api/game/create", ip),
                "esgotar o limite de login nao deveria impedir de criar partida");
    }

    @Test
    void ipsDistintosNaoCompartilhamLimite() throws Exception {
        for (int i = 0; i < 40; i++) {
            chamar("POST", "/api/auth/login", "1.1.1.1");
        }
        assertNotEquals(429, chamar("POST", "/api/auth/login", "2.2.2.2"),
                "cada IP deve ter seu proprio limite");
    }

    @Test
    void respeitaXForwardedForAtrasDeProxy() throws Exception {
        // Em producao (Render) todos chegam pelo mesmo remoteAddr do proxy;
        // sem usar X-Forwarded-For todos dividiriam o mesmo bucket.
        for (int i = 0; i < 40; i++) {
            MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/auth/login");
            r.setRemoteAddr("10.0.0.1");
            r.addHeader("X-Forwarded-For", "203.0.113.5");
            filter.doFilter(r, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest outro = new MockHttpServletRequest("POST", "/api/auth/login");
        outro.setRemoteAddr("10.0.0.1");
        outro.addHeader("X-Forwarded-For", "203.0.113.99");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(outro, resp, new MockFilterChain());

        assertNotEquals(429, resp.getStatus(),
                "clientes diferentes atras do mesmo proxy devem ter limites separados");
    }

    @Test
    void informaTokensRestantes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/game/open");
        request.setRemoteAddr("8.8.8.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertNotNull(response.getHeader("X-Rate-Limit-Remaining"));
    }

    @Test
    void naoFiltraWebSocketNemActuator() {
        assertTrue(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/ws/info")));
        assertTrue(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/actuator/health")));
        assertFalse(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/game/open")));
    }
}

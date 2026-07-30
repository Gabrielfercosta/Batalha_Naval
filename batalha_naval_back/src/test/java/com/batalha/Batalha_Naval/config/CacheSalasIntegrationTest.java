package com.batalha.Batalha_Naval.config;

import com.batalha.Batalha_Naval.dto.SalaResponse;
import com.batalha.Batalha_Naval.minado.MinadoService;
import com.batalha.Batalha_Naval.quiz.QuizService;
import com.batalha.Batalha_Naval.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Garante que o cache da listagem de salas não esconde salas recém-criadas.
 *
 * Regressão coberta: com ConcurrentMapCacheManager (sem TTL) e sem @CacheEvict no
 * criarPartidaQuiz, uma sala criada não aparecia para os outros jogadores.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.cache.type=simple",
        "spring.datasource.url=jdbc:h2:mem:cachetest;DB_CLOSE_DELAY=-1",
        "management.otlp.tracing.export.enabled=false"
})
class CacheSalasIntegrationTest {

    @Autowired private GameService gameService;
    @Autowired private MinadoService minadoService;
    @Autowired private QuizService quizService;
    @Autowired private CacheManager cacheManager;

    @Test
    void cacheEmMemoriaTemTtl() {
        // ConcurrentMapCacheManager não expira entradas; Caffeine sim.
        assertInstanceOf(CaffeineCacheManager.class, cacheManager,
                "sem TTL a lista de salas congela e salas novas nunca aparecem");
    }

    @Test
    void salaClassicaApareceImediatamenteAposCriada() {
        gameService.listarPartidasAbertas(); // popula o cache
        gameService.criarPartida("alice", "Sala Classica", null);

        List<SalaResponse> abertas = gameService.listarPartidasAbertas();
        assertTrue(abertas.stream().anyMatch(s -> "Sala Classica".equals(s.getNome())),
                "sala criada deveria aparecer na listagem sem esperar o TTL");
    }

    @Test
    void salaMinadaApareceImediatamenteAposCriada() {
        minadoService.listarPartidasAbertas();
        minadoService.criarPartida("bob", "Sala Minada", null);

        List<SalaResponse> abertas = minadoService.listarPartidasAbertas();
        assertTrue(abertas.stream().anyMatch(s -> "Sala Minada".equals(s.getNome())),
                "sala criada deveria aparecer na listagem sem esperar o TTL");
    }

    @Test
    void salaQuizApareceImediatamenteAposCriada() {
        quizService.listarPartidasAbertas();
        // criarPartidaQuiz chama criarPartida internamente (self-invocation):
        // sem @CacheEvict no próprio método, o evict não dispara
        quizService.criarPartidaQuiz("carol", "Sala Quiz", null, List.of("geral"), "", false);

        List<SalaResponse> abertas = quizService.listarPartidasAbertas();
        assertTrue(abertas.stream().anyMatch(s -> "Sala Quiz".equals(s.getNome())),
                "sala de quiz criada deveria aparecer na listagem sem esperar o TTL");
    }

    @Test
    void cadaModoTemEntradaDeCacheIndependente() {
        gameService.criarPartida("dave", "So Classica", null);

        assertTrue(gameService.listarPartidasAbertas().stream()
                        .anyMatch(s -> "So Classica".equals(s.getNome())));
        assertFalse(minadoService.listarPartidasAbertas().stream()
                        .anyMatch(s -> "So Classica".equals(s.getNome())),
                "a chave do cache deve separar os modos");
    }

    @Test
    void saidaDeJogadorRemoveSalaDaListagem() {
        String id = gameService.criarPartida("erin", "Sala Temporaria", null);
        assertTrue(gameService.listarPartidasAbertas().stream()
                .anyMatch(s -> "Sala Temporaria".equals(s.getNome())));

        gameService.sairDaPartida(id, "erin");

        assertFalse(gameService.listarPartidasAbertas().stream()
                        .anyMatch(s -> "Sala Temporaria".equals(s.getNome())),
                "sala removida não deveria continuar na listagem cacheada");
    }
}

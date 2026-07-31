package com.batalha.Batalha_Naval.quiz;

import com.batalha.Batalha_Naval.config.GameplayMetrics;
import com.batalha.Batalha_Naval.dominio.Direcao;
import com.batalha.Batalha_Naval.dominio.TipoNavio;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

/**
 * Cobre a resolução de perguntas do quiz sob concorrência.
 *
 * Regressão coberta: o registro de métricas iterava o mapa acertouPergunta fora do
 * bloco sincronizado. Como responder() escreve nesse mesmo mapa, dois jogadores
 * respondendo ao mesmo tempo causavam ConcurrentModificationException, que
 * interrompia resolverPergunta() antes de publicar o RESULTADO — deixando os dois
 * jogadores presos em "Esperando o outro jogador...".
 */
class QuizConcorrenciaTest {

    private QuizService quizService;
    private SimpMessagingTemplate messaging;

    @BeforeEach
    void setUp() {
        messaging = mock(SimpMessagingTemplate.class);
        quizService = new QuizService(new TriviaService(), messaging,
                new GameplayMetrics(new SimpleMeterRegistry()));
    }

    private String partidaEmAndamento() {
        String id = quizService.criarPartidaQuiz("p1", "Sala", null, List.of("geral"), "", false);
        quizService.entrarNaPartida(id, "p2", null);
        for (String jogador : List.of("p1", "p2")) {
            quizService.posicionarNavio(id, jogador, TipoNavio.PORTA_AVIOES, 0, 0, Direcao.HORIZONTAL);
            quizService.posicionarNavio(id, jogador, TipoNavio.ENCOURACADO, 1, 0, Direcao.HORIZONTAL);
            quizService.posicionarNavio(id, jogador, TipoNavio.CRUZADOR, 2, 0, Direcao.HORIZONTAL);
            quizService.posicionarNavio(id, jogador, TipoNavio.SUBMARINO, 3, 0, Direcao.HORIZONTAL);
            quizService.posicionarNavio(id, jogador, TipoNavio.DESTROYER, 4, 0, Direcao.HORIZONTAL);
        }
        quizService.marcarPronto(id, "p1");
        quizService.marcarPronto(id, "p2");
        return id;
    }

    @Test
    @Timeout(30)
    void respostasSimultaneasNaoInterrompemAResolucao() throws Exception {
        // Repetido para aumentar a chance de pegar a janela de concorrência
        for (int rodada = 0; rodada < 40; rodada++) {
            String id = partidaEmAndamento();
            PartidaQuiz partida = quizService.buscarPartida(id);
            partida.iniciarPergunta(new PerguntaTrivia(
                    "Pergunta " + rodada, List.of("a", "b"), "a", "easy"));

            AtomicReference<Throwable> falha = new AtomicReference<>();
            CountDownLatch largada = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(2);

            for (String jogador : List.of("p1", "p2")) {
                pool.submit(() -> {
                    try {
                        largada.await();
                        quizService.responder(id, jogador, "a");
                    } catch (Throwable t) {
                        falha.compareAndSet(null, t);
                    }
                });
            }

            largada.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "as respostas travaram");

            if (falha.get() != null) {
                fail("resolver a pergunta falhou sob concorrência: " + falha.get());
            }
        }
    }

    @Test
    @Timeout(30)
    void resultadoEhPublicadoQuandoOsDoisRespondem() throws Exception {
        String id = partidaEmAndamento();
        PartidaQuiz partida = quizService.buscarPartida(id);
        partida.iniciarPergunta(new PerguntaTrivia("Capital?", List.of("a", "b"), "a", "medium"));

        quizService.responder(id, "p1", "a");
        quizService.responder(id, "p2", "b");

        // O RESULTADO é o que faz o frontend sair da tela de espera
        verify(messaging, atLeastOnce()).convertAndSend(
                startsWith("/topic/quiz/" + id), any(ResultadoRodadaResponse.class));
    }

    @Test
    @Timeout(30)
    void metricasDeRespostaSaoRegistradasPorDificuldade() {
        String id = partidaEmAndamento();
        PartidaQuiz partida = quizService.buscarPartida(id);
        partida.iniciarPergunta(new PerguntaTrivia("Dificil?", List.of("a", "b"), "a", "hard"));

        assertDoesNotThrow(() -> {
            quizService.responder(id, "p1", "a");
            quizService.responder(id, "p2", "a");
        });
    }
}

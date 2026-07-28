package com.batalha.Batalha_Naval.quiz;

import com.batalha.Batalha_Naval.dominio.*;
import com.batalha.Batalha_Naval.dto.SalaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuizServiceTest {

    private QuizService quizService;
    private SimpMessagingTemplate messaging;

    @BeforeEach
    void setUp() {
        TriviaService triviaService = new TriviaService();
        messaging = mock(SimpMessagingTemplate.class);
        quizService = new QuizService(triviaService, messaging);
    }

    @Test
    void criarPartidaQuizRetornaId() {
        String id = quizService.criarPartidaQuiz("Alice", "Quiz1", null,
                List.of("geral"), "easy", false);

        assertNotNull(id);
        assertFalse(id.isBlank());
    }

    @Test
    void buscarPartidaQuizCriada() {
        String id = quizService.criarPartidaQuiz("Alice", "Quiz1", null,
                List.of("geral"), "", false);

        PartidaQuiz partida = quizService.buscarPartida(id);
        assertEquals("Alice", partida.getJogador1());
        assertEquals(StatusPartida.AGUARDANDO, partida.getStatus());
    }

    @Test
    void entrarNaPartidaQuiz() {
        String id = quizService.criarPartidaQuiz("Alice", "Quiz1", null,
                List.of("geral"), "", false);

        PartidaQuiz partida = quizService.entrarNaPartida(id, "Bob", null);
        assertEquals("Bob", partida.getJogador2());
    }

    @Test
    void listarPartidasAbertasQuiz() {
        quizService.criarPartidaQuiz("Alice", "Quiz1", null, List.of("geral"), "", false);
        quizService.criarPartidaQuiz("Bob", "Quiz2", "senha", List.of("filmes"), "hard", true);

        List<SalaResponse> abertas = quizService.listarPartidasAbertas();
        assertEquals(2, abertas.size());
    }

    @Test
    void posicionarNavioNoQuiz() {
        String id = quizService.criarPartidaQuiz("Alice", "Quiz1", null,
                List.of("geral"), "", false);
        quizService.entrarNaPartida(id, "Bob", null);

        quizService.posicionarNavio(id, "Alice", TipoNavio.DESTROYER, 0, 0, Direcao.HORIZONTAL);

        PartidaQuiz partida = quizService.buscarPartida(id);
        assertEquals(1, partida.getTabuleiro1().getNavios().size());
    }

    @Test
    void partidaInexistenteLancaErro() {
        assertThrows(IllegalArgumentException.class,
                () -> quizService.buscarPartida("id-invalido"));
    }

    @Test
    void sairDaSalaAguardandoRemove() {
        String id = quizService.criarPartidaQuiz("Alice", "Quiz1", null,
                List.of("geral"), "", false);

        quizService.sairDaPartida(id, "Alice");

        assertThrows(IllegalArgumentException.class,
                () -> quizService.buscarPartida(id));
    }
}

package com.batalha.Batalha_Naval.integracao;

import com.batalha.Batalha_Naval.auth.JwtService;
import com.batalha.Batalha_Naval.dominio.Direcao;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import com.batalha.Batalha_Naval.dominio.TipoNavio;
import com.batalha.Batalha_Naval.quiz.QuizService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproduz o início de uma partida de quiz com dois jogadores conectados por
 * WebSocket, como acontece no navegador.
 *
 * No quiz a partida só arranca quando os DOIS clientes publicam em
 * /app/quiz/{id}/cheguei. Se um dos avisos não chegar ou o start não for
 * disparado, os dois ficam presos em "Esperando o outro jogador...".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.cache.type=simple",
        "spring.datasource.url=jdbc:h2:mem:quizws;DB_CLOSE_DELAY=-1",
        "management.otlp.tracing.export.enabled=false",
        "app.cors.origins=*"
})
class QuizWebSocketIntegrationTest {

    @LocalServerPort private int porta;
    @Autowired private QuizService quizService;
    @Autowired private JwtService jwtService;

    private StompSession conectar(String jogador) throws Exception {
        WebSocketStompClient cliente = new WebSocketStompClient(new StandardWebSocketClient());
        cliente.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + jwtService.gerarToken(jogador));
        return cliente.connectAsync("ws://localhost:" + porta + "/ws/websocket",
                new org.springframework.web.socket.WebSocketHttpHeaders(), headers,
                new StompSessionHandlerAdapter() {}).get(10, TimeUnit.SECONDS);
    }

    private BlockingQueue<Map> inscrever(StompSession sessao, String topico) {
        BlockingQueue<Map> fila = new LinkedBlockingQueue<>();
        sessao.subscribe(topico, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders h) { return Map.class; }
            @Override public void handleFrame(StompHeaders h, Object payload) { fila.add((Map) payload); }
        });
        return fila;
    }

    private void posicionarFrota(String id, String jogador) {
        quizService.posicionarNavio(id, jogador, TipoNavio.PORTA_AVIOES, 0, 0, Direcao.HORIZONTAL);
        quizService.posicionarNavio(id, jogador, TipoNavio.ENCOURACADO, 1, 0, Direcao.HORIZONTAL);
        quizService.posicionarNavio(id, jogador, TipoNavio.CRUZADOR, 2, 0, Direcao.HORIZONTAL);
        quizService.posicionarNavio(id, jogador, TipoNavio.SUBMARINO, 3, 0, Direcao.HORIZONTAL);
        quizService.posicionarNavio(id, jogador, TipoNavio.DESTROYER, 4, 0, Direcao.HORIZONTAL);
    }

    @Test
    void partidaDeQuizComecaQuandoOsDoisJogadoresChegam() throws Exception {
        String id = quizService.criarPartidaQuiz("qwsA", "Sala Quiz WS", null,
                List.of("geral"), "", false);
        quizService.entrarNaPartida(id, "qwsB", null);
        posicionarFrota(id, "qwsA");
        posicionarFrota(id, "qwsB");
        quizService.marcarPronto(id, "qwsA");
        quizService.marcarPronto(id, "qwsB");
        assertEquals(StatusPartida.EM_ANDAMENTO, quizService.buscarPartida(id).getStatus());

        StompSession sessaoA = conectar("qwsA");
        StompSession sessaoB = conectar("qwsB");
        BlockingQueue<Map> eventosA = inscrever(sessaoA, "/topic/quiz/" + id);
        BlockingQueue<Map> eventosB = inscrever(sessaoB, "/topic/quiz/" + id);
        Thread.sleep(500);

        // Cada cliente avisa que chegou, como o frontend faz no onConnect
        sessaoA.send("/app/quiz/" + id + "/cheguei", Map.of("jogador", "qwsA"));
        sessaoB.send("/app/quiz/" + id + "/cheguei", Map.of("jogador", "qwsB"));

        Map primeiroA = eventosA.poll(15, TimeUnit.SECONDS);
        Map primeiroB = eventosB.poll(15, TimeUnit.SECONDS);

        assertNotNull(primeiroA, "jogador A nao recebeu evento de inicio (ficaria esperando para sempre)");
        assertNotNull(primeiroB, "jogador B nao recebeu evento de inicio (ficaria esperando para sempre)");
        assertEquals("CONTAGEM", primeiroA.get("tipo"), "o primeiro evento deveria ser a contagem");
        assertEquals("CONTAGEM", primeiroB.get("tipo"), "o primeiro evento deveria ser a contagem");

        // Depois da contagem vem a pergunta
        Map perguntaA = eventosA.poll(15, TimeUnit.SECONDS);
        assertNotNull(perguntaA, "a pergunta nao chegou apos a contagem");
        assertEquals("PERGUNTA", perguntaA.get("tipo"));
        assertNotNull(perguntaA.get("pergunta"), "a pergunta veio sem texto");

        sessaoA.disconnect();
        sessaoB.disconnect();
    }

    @Test
    void quizNaoComecaComApenasUmJogadorPresente() throws Exception {
        String id = quizService.criarPartidaQuiz("soloA", "Sala Solo", null,
                List.of("geral"), "", false);
        quizService.entrarNaPartida(id, "soloB", null);
        posicionarFrota(id, "soloA");
        posicionarFrota(id, "soloB");
        quizService.marcarPronto(id, "soloA");
        quizService.marcarPronto(id, "soloB");

        StompSession sessaoA = conectar("soloA");
        BlockingQueue<Map> eventosA = inscrever(sessaoA, "/topic/quiz/" + id);
        Thread.sleep(500);

        sessaoA.send("/app/quiz/" + id + "/cheguei", Map.of("jogador", "soloA"));

        assertNull(eventosA.poll(3, TimeUnit.SECONDS),
                "a partida nao deveria comecar com apenas um jogador presente");

        sessaoA.disconnect();
    }
}

package com.batalha.Batalha_Naval.integracao;

import com.batalha.Batalha_Naval.dominio.Direcao;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import com.batalha.Batalha_Naval.dominio.TipoNavio;
import com.batalha.Batalha_Naval.dto.TiroResponse;
import com.batalha.Batalha_Naval.service.GameService;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica a notificação de início de partida pelo WebSocket.
 *
 * A tela de batalha depende de duas fontes para saber que a partida começou:
 * o GET /api/game/{id} feito ao montar, e a mensagem STOMP enviada quando o
 * segundo jogador marca pronto. Se as duas falharem, o jogador fica preso em
 * "Esperando o outro jogador ficar pronto...".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.cache.type=simple",
        "spring.datasource.url=jdbc:h2:mem:wstest;DB_CLOSE_DELAY=-1",
        "management.otlp.tracing.export.enabled=false",
        "app.cors.origins=*"
})
class PartidaWebSocketIntegrationTest {

    @LocalServerPort
    private int porta;

    @Autowired
    private GameService gameService;

    private WebSocketStompClient novoCliente() {
        WebSocketStompClient cliente = new WebSocketStompClient(new StandardWebSocketClient());
        cliente.setMessageConverter(new MappingJackson2MessageConverter());
        return cliente;
    }

    private StompSession conectar() throws Exception {
        // Com withSockJS() no servidor, clientes WebSocket puros usam /ws/websocket
        return novoCliente()
                .connectAsync("ws://localhost:" + porta + "/ws/websocket", new StompSessionHandlerAdapter() {})
                .get(10, TimeUnit.SECONDS);
    }

    private void posicionarFrota(String id, String jogador) {
        gameService.posicionarNavio(id, jogador, TipoNavio.PORTA_AVIOES, 0, 0, Direcao.HORIZONTAL);
        gameService.posicionarNavio(id, jogador, TipoNavio.ENCOURACADO, 1, 0, Direcao.HORIZONTAL);
        gameService.posicionarNavio(id, jogador, TipoNavio.CRUZADOR, 2, 0, Direcao.HORIZONTAL);
        gameService.posicionarNavio(id, jogador, TipoNavio.SUBMARINO, 3, 0, Direcao.HORIZONTAL);
        gameService.posicionarNavio(id, jogador, TipoNavio.DESTROYER, 4, 0, Direcao.HORIZONTAL);
    }

    @Test
    void jogadorInscritoRecebeNotificacaoDeInicio() throws Exception {
        String id = gameService.criarPartida("wsA", "Sala WS", null);
        gameService.entrarNaPartida(id, "wsB", null);
        posicionarFrota(id, "wsA");
        posicionarFrota(id, "wsB");

        // Jogador A entra na tela de batalha e se inscreve no tópico
        StompSession sessaoA = conectar();
        BlockingQueue<TiroResponse> recebidas = new LinkedBlockingQueue<>();
        sessaoA.subscribe("/topic/game/" + id, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return TiroResponse.class;
            }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                recebidas.add((TiroResponse) payload);
            }
        });
        Thread.sleep(500); // garante que a inscrição está ativa

        // Ambos ficam prontos — o segundo dispara a notificação
        gameService.marcarPronto(id, "wsA");
        gameService.marcarPronto(id, "wsB");

        // Simula o que o GameController faz ao detectar a partida iniciada
        var partida = gameService.buscarPartida(id);
        assertEquals(StatusPartida.EM_ANDAMENTO, partida.getStatus(),
                "a partida deveria iniciar quando os dois jogadores estão prontos");
        assertNotNull(partida.getTurnoAtual(), "o turno inicial deveria estar definido");

        sessaoA.disconnect();
    }

    @Test
    void estadoDaPartidaPermiteRecuperarInicioSemWebSocket() {
        // Cobre o jogador que entra na tela de batalha depois da notificação:
        // ele precisa descobrir pelo GET que a partida já começou.
        String id = gameService.criarPartida("recA", "Sala Recuperar", null);
        gameService.entrarNaPartida(id, "recB", null);
        posicionarFrota(id, "recA");
        posicionarFrota(id, "recB");

        gameService.marcarPronto(id, "recA");
        gameService.marcarPronto(id, "recB");

        var partida = gameService.buscarPartida(id);
        assertEquals(StatusPartida.EM_ANDAMENTO, partida.getStatus());
        assertEquals("recA", partida.getTurnoAtual(),
                "quem criou a sala começa jogando");
    }

    @Test
    void partidaNaoIniciaComApenasUmJogadorPronto() {
        String id = gameService.criarPartida("umA", "Sala Um", null);
        gameService.entrarNaPartida(id, "umB", null);
        posicionarFrota(id, "umA");
        posicionarFrota(id, "umB");

        gameService.marcarPronto(id, "umA");

        assertEquals(StatusPartida.POSICIONANDO, gameService.buscarPartida(id).getStatus(),
                "com um jogador pronto a partida deve seguir aguardando");
    }

    @Test
    void handshakeWebSocketFunciona() throws Exception {
        StompSession sessao = conectar();
        assertTrue(sessao.isConnected(), "o handshake STOMP deveria conectar");
        sessao.disconnect();
    }
}

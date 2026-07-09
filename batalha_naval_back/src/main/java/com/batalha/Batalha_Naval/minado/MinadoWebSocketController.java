package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.dto.ErroResponse;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class MinadoWebSocketController {

    private final MinadoService minadoService;
    private final SimpMessagingTemplate messagingTemplate;

    public MinadoWebSocketController(MinadoService minadoService,
                                     SimpMessagingTemplate messagingTemplate) {
        this.minadoService = minadoService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/minado/{gameId}/tiro")
    public void atirar(@DestinationVariable String gameId, TiroMinadoRequest request, Principal principal) {
        String jogador = principal.getName();
        int linha = request.getLinha();
        int coluna = request.getColuna();

        try {
            PartidaMinada partida = minadoService.buscarPartida(gameId);
            TabuleiroMinado oponente = jogador.equals(partida.getJogador1())
                    ? partida.getTabuleiro2() : partida.getTabuleiro1();

            Set<String> antesSet = new HashSet<>();
            for (int[] pos : oponente.casasReveladas()) {
                antesSet.add(pos[0] + "-" + pos[1]);
            }

            ResultadoTiroMinado resultado = minadoService.atirar(gameId, jogador, linha, coluna);

            List<CasaRevelada> novas = new ArrayList<>();
            for (int[] pos : oponente.casasReveladas()) {
                String chave = pos[0] + "-" + pos[1];
                if (!antesSet.contains(chave) && oponente.getEstado(pos[0], pos[1]) == EstadoCasa.AGUA) {
                    Pista pista = oponente.contarVizinhos(pos[0], pos[1]);
                    novas.add(new CasaRevelada(pos[0], pos[1], pista.getMinas(), pista.getNavios()));
                }
            }

            partida = minadoService.buscarPartida(gameId);
            TiroMinadoResponse response = new TiroMinadoResponse(
                    jogador, linha, coluna, resultado,
                    partida.getTurnoAtual(), partida.getStatus(), partida.getVencedor(),
                    novas);

            messagingTemplate.convertAndSend("/topic/minado/" + gameId, response);

        } catch (Exception e) {
            messagingTemplate.convertAndSend(
                    "/topic/minado/" + gameId + "/erro/" + jogador,
                    new ErroResponse(e.getMessage()));
        }
    }

    @MessageMapping("/minado/{gameId}/cheguei")
    public void cheguei(@DestinationVariable String gameId, Principal principal) {
        String jogador = principal.getName();
        try {
            PartidaMinada partida = minadoService.buscarPartida(gameId);
            if (partida.registrarChegada(jogador)) {
                messagingTemplate.convertAndSend("/topic/minado/" + gameId + "/contagem", (Object) Map.of("segundos", 5));
            }
        } catch (Exception e) {
        }
    }

}

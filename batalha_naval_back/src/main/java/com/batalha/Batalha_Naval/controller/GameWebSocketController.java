package com.batalha.Batalha_Naval.controller;

import com.batalha.Batalha_Naval.dominio.Coordenada;
import com.batalha.Batalha_Naval.dominio.Partida;
import com.batalha.Batalha_Naval.dominio.ResultadoTiro;
import com.batalha.Batalha_Naval.dto.ErroResponse;
import com.batalha.Batalha_Naval.dto.TiroRequest;
import com.batalha.Batalha_Naval.dto.TiroResponse;
import com.batalha.Batalha_Naval.service.GameService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class GameWebSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{gameId}/tiro")
    public void atirar(@DestinationVariable String gameId, TiroRequest request) {
        try {
            Coordenada tiro = new Coordenada(request.getLinha(), request.getColuna());

            ResultadoTiro resultado = gameService.atirar(gameId, request.getJogador(), tiro);

            Partida partida = gameService.buscarPartida(gameId);

            TiroResponse response = new TiroResponse(
                    request.getJogador(),
                    request.getLinha(),
                    request.getColuna(),
                    resultado,
                    partida.getTurnoAtual(),
                    partida.getStatus(),
                    partida.getVencedor()
            );

            messagingTemplate.convertAndSend("/topic/game/" + gameId, response);

        } catch (Exception e) {
            messagingTemplate.convertAndSend(
                    "/topic/game/" + gameId + "/erro/" + request.getJogador(),
                    new ErroResponse(e.getMessage())
            );
        }
    }

}

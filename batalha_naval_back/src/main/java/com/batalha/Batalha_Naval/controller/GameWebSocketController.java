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

import java.security.Principal;

@Controller
@AllArgsConstructor
public class GameWebSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{gameId}/tiro")
    public void atirar(@DestinationVariable String gameId, TiroRequest request, Principal principal) {
        String jogador = principal.getName();
        try {
            Coordenada tiro = new Coordenada(request.getLinha(), request.getColuna());

            ResultadoTiro resultado = gameService.atirar(gameId, jogador, tiro);

            Partida partida = gameService.buscarPartida(gameId);

            TiroResponse response = new TiroResponse(
                    jogador,
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
                    "/topic/game/" + gameId + "/erro/" + jogador,
                    new ErroResponse(e.getMessage())
            );
        }
    }

}

package com.batalha.Batalha_Naval.controller;

import com.batalha.Batalha_Naval.dominio.Partida;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import com.batalha.Batalha_Naval.dto.*;
import com.batalha.Batalha_Naval.service.GameService;
import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/game")
@AllArgsConstructor
@Timed(value = "game.controller", description = "Tempo dos endpoints de jogo")
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/create")
    public PartidaResponse criar(@RequestBody CriarPartidaRequest request, Principal principal) {
        String gameId = gameService.criarPartida(principal.getName(), request.getNome(), request.getSenha());
        Partida partida = gameService.buscarPartida(gameId);
        return new PartidaResponse(gameId, partida);
    }

    @PostMapping("/{gameId}/join")
    public PartidaResponse entrar(@PathVariable String gameId, @RequestBody EntrarPartidaRequest request, Principal principal) {
        Partida partida = gameService.entrarNaPartida(gameId, principal.getName(), request.getSenha());
        return new PartidaResponse(gameId, partida);
    }

    @GetMapping("/open")
    public List<SalaResponse> listarAbertas() {
        return gameService.listarPartidasAbertas();
    }

    @GetMapping("/{gameId}")
    public PartidaResponse buscar(@PathVariable String gameId) {
        Partida partida = gameService.buscarPartida(gameId);
        return new PartidaResponse(gameId, partida);
    }

    @PostMapping("/{gameId}/posicionar")
    public PartidaResponse posicionar(@PathVariable String gameId, @RequestBody PosicionarNavioRequest request, Principal principal) {
        gameService.posicionarNavio(
                gameId,
                principal.getName(),
                request.getTipo(),
                request.getLinha(),
                request.getColuna(),
                request.getDirecao()
        );
        Partida partida = gameService.buscarPartida(gameId);
        return new PartidaResponse(gameId, partida);
    }

    @PostMapping("/{gameId}/pronto")
    public PartidaResponse pronto(@PathVariable String gameId, Principal principal) {
        Partida partida = gameService.marcarPronto(gameId, principal.getName());

        if (partida.getStatus() == StatusPartida.EM_ANDAMENTO) {
            TiroResponse inicio = new TiroResponse(
                    null, -1, -1, null,
                    partida.getTurnoAtual(),
                    partida.getStatus(),
                    partida.getVencedor(),
                    null
            );
            messagingTemplate.convertAndSend("/topic/game/" + gameId, inicio);
        }

        return new PartidaResponse(gameId, partida);
    }

    @PostMapping("/{gameId}/sair")
    public void sair(@PathVariable String gameId, Principal principal) {
        Partida partida = gameService.sairDaPartida(gameId, principal.getName());
        if (partida != null) {
            TiroResponse aviso = new TiroResponse(
                    null, -1, -1, null,
                    partida.getTurnoAtual(),
                    partida.getStatus(),
                    partida.getVencedor(),
                    null
            );
            messagingTemplate.convertAndSend("/topic/game/" + gameId, aviso);
        }
    }
}

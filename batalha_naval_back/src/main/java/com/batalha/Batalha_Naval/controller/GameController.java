package com.batalha.Batalha_Naval.controller;

import com.batalha.Batalha_Naval.dominio.Partida;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import com.batalha.Batalha_Naval.dto.*;
import com.batalha.Batalha_Naval.service.GameService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/game")
@AllArgsConstructor
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/create")
    public PartidaResponse criar(@RequestBody CriarPartidaRequest request) {
        String gameId = gameService.criarPartida(request.getJogador(), request.getNome(), request.getSenha());
        Partida partida = gameService.buscarPartida(gameId);
        return new PartidaResponse(gameId, partida);
    }

    @PostMapping("/{gameId}/join")
    public PartidaResponse entrar(@PathVariable String gameId, @RequestBody EntrarPartidaRequest request) {
        Partida partida = gameService.entrarNaPartida(gameId, request.getJogador(), request.getSenha());
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
    public PartidaResponse posicionar(@PathVariable String gameId, @RequestBody PosicionarNavioRequest request) {
        gameService.posicionarNavio(
                gameId,
                request.getJogador(),
                request.getTipo(),
                request.getLinha(),
                request.getColuna(),
                request.getDirecao()
        );
        Partida partida = gameService.buscarPartida(gameId);
        return new PartidaResponse(gameId, partida);
    }

    @PostMapping("/{gameId}/pronto")
    public PartidaResponse pronto(@PathVariable String gameId, @RequestBody EntrarPartidaRequest request) {
        Partida partida = gameService.marcarPronto(gameId, request.getJogador());

        if (partida.getStatus() == StatusPartida.EM_ANDAMENTO) {
            TiroResponse inicio = new TiroResponse(
                    null, -1, -1, null,
                    partida.getTurnoAtual(),
                    partida.getStatus(),
                    partida.getVencedor()
            );
            messagingTemplate.convertAndSend("/topic/game/" + gameId, inicio);
        }

        return new PartidaResponse(gameId, partida);
    }

    @PostMapping("/{gameId}/iniciar")
    public PartidaResponse iniciar(@PathVariable String gameId) {
        gameService.iniciarBatalha(gameId);
        Partida partida = gameService.buscarPartida(gameId);
        return new PartidaResponse(gameId, partida);
    }

}

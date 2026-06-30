package com.batalha.Batalha_Naval.controller;

import com.batalha.Batalha_Naval.dominio.Partida;
import com.batalha.Batalha_Naval.dto.CriarPartidaRequest;
import com.batalha.Batalha_Naval.dto.EntrarPartidaRequest;
import com.batalha.Batalha_Naval.dto.PartidaResponse;
import com.batalha.Batalha_Naval.dto.PosicionarNavioRequest;
import com.batalha.Batalha_Naval.service.GameService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/create")
    public PartidaResponse criar(@RequestBody CriarPartidaRequest request) {
        String gameId = gameService.criarPartida(request.getJogador());
        Partida partida = gameService.buscarPartida(gameId);
        return new PartidaResponse(gameId, partida);
    }

    @PostMapping("/{gameId}/join")
    public PartidaResponse entrar(@PathVariable String gameId, @RequestBody EntrarPartidaRequest request) {
        Partida partida = gameService.entrarNaPartida(gameId, request.getJogador());
        return new PartidaResponse(gameId, partida);
    }

    @GetMapping("/open")
    public List<String> listarAbertas() {
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

    @PostMapping("/{gameId}/iniciar")
    public PartidaResponse iniciar(@PathVariable String gameId) {
        gameService.iniciarBatalha(gameId);
        Partida partida = gameService.buscarPartida(gameId);
        return new PartidaResponse(gameId, partida);
    }

}

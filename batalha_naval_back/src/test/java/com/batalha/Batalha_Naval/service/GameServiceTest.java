package com.batalha.Batalha_Naval.service;

import com.batalha.Batalha_Naval.dominio.*;
import com.batalha.Batalha_Naval.dto.SalaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService();
    }

    @Test
    void criarPartidaRetornaId() {
        String id = gameService.criarPartida("Alice", "Sala1", null);
        assertNotNull(id);
        assertFalse(id.isBlank());
    }

    @Test
    void buscarPartidaExistente() {
        String id = gameService.criarPartida("Alice", "Sala1", null);
        Partida p = gameService.buscarPartida(id);
        assertEquals("Alice", p.getJogador1());
    }

    @Test
    void buscarPartidaInexistenteLancaErro() {
        assertThrows(IllegalArgumentException.class,
                () -> gameService.buscarPartida("id-invalido"));
    }

    @Test
    void entrarNaPartida() {
        String id = gameService.criarPartida("Alice", "Sala1", null);
        Partida p = gameService.entrarNaPartida(id, "Bob", null);
        assertEquals("Bob", p.getJogador2());
        assertEquals(StatusPartida.POSICIONANDO, p.getStatus());
    }

    @Test
    void listarPartidasAbertas() {
        gameService.criarPartida("Alice", "Sala1", null);
        gameService.criarPartida("Bob", "Sala2", "senha");

        List<SalaResponse> abertas = gameService.listarPartidasAbertas();
        assertEquals(2, abertas.size());
    }

    @Test
    void listarNaoMostraPartidasComJogador2() {
        String id = gameService.criarPartida("Alice", "Sala1", null);
        gameService.entrarNaPartida(id, "Bob", null);

        List<SalaResponse> abertas = gameService.listarPartidasAbertas();
        assertEquals(0, abertas.size());
    }

    @Test
    void posicionarNavioFunciona() {
        String id = gameService.criarPartida("Alice", "Sala1", null);
        gameService.posicionarNavio(id, "Alice", TipoNavio.DESTROYER, 0, 0, Direcao.HORIZONTAL);

        Partida p = gameService.buscarPartida(id);
        assertEquals(1, p.getTabuleiro1().getNavios().size());
    }

    @Test
    void sairDaPartidaEmAndamentoDaVitoria() {
        String id = gameService.criarPartida("Alice", "Sala1", null);
        gameService.entrarNaPartida(id, "Bob", null);
        posicionarFrotaCompleta(id, "Alice");
        posicionarFrotaCompleta(id, "Bob");
        gameService.marcarPronto(id, "Alice");
        gameService.marcarPronto(id, "Bob");

        Partida p = gameService.sairDaPartida(id, "Alice");
        assertEquals(StatusPartida.FINALIZADA, p.getStatus());
        assertEquals("Bob", p.getVencedor());
    }

    @Test
    void sairDaSalaAguardandoRemovePartida() {
        String id = gameService.criarPartida("Alice", "Sala1", null);
        gameService.sairDaPartida(id, "Alice");

        assertThrows(IllegalArgumentException.class, () -> gameService.buscarPartida(id));
    }

    private void posicionarFrotaCompleta(String id, String jogador) {
        gameService.posicionarNavio(id, jogador, TipoNavio.PORTA_AVIOES, 0, 0, Direcao.HORIZONTAL);
        gameService.posicionarNavio(id, jogador, TipoNavio.ENCOURACADO, 1, 0, Direcao.HORIZONTAL);
        gameService.posicionarNavio(id, jogador, TipoNavio.CRUZADOR, 2, 0, Direcao.HORIZONTAL);
        gameService.posicionarNavio(id, jogador, TipoNavio.SUBMARINO, 3, 0, Direcao.HORIZONTAL);
        gameService.posicionarNavio(id, jogador, TipoNavio.DESTROYER, 4, 0, Direcao.HORIZONTAL);
    }
}

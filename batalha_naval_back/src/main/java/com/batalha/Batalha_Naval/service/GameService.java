package com.batalha.Batalha_Naval.service;

import com.batalha.Batalha_Naval.dominio.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class GameService {

    private final Map<String, Partida> partidas = new ConcurrentHashMap<>();

    public String criarPartida(String jogador1) {
        String id = UUID.randomUUID().toString();
        Partida partida = new Partida(jogador1);
        partidas.put(id, partida);
        return id;
    }

    public Partida buscarPartida(String gameId) {
        Partida partida = partidas.get(gameId);
        if (partida == null) {
            throw new IllegalArgumentException("Partida não encontrada: " + gameId);
        }
        return partida;
    }

    public Partida entrarNaPartida(String gameId, String jogador2) {
        Partida partida = buscarPartida(gameId);
        partida.entrar(jogador2);
        return partida;
    }

    public List<String> listarPartidasAbertas() {
        List<String> abertas = new ArrayList<>();
        for (Map.Entry<String, Partida> entry : partidas.entrySet()) {
            if (entry.getValue().getStatus() == StatusPartida.AGUARDANDO) {
                abertas.add(entry.getKey());
            }
        }
        return abertas;
    }


    public ResultadoTiro atirar(String gameId, String jogador, Coordenada tiro) {
        Partida partida = buscarPartida(gameId);
        return partida.atirar(jogador, tiro);
    }

    public void posicionarNavio(String gameId, String jogador, TipoNavio tipo, int linha, int coluna, Direcao direcao) {
        Partida partida = buscarPartida(gameId);

        Navio navio = FabricaNavio.criar(tipo, linha, coluna, direcao);

        Tabuleiro tabuleiro = jogador.equals(partida.getJogador1()) ? partida.getTabuleiro1() : partida.getTabuleiro2();

        tabuleiro.posicionarNavio(navio);
    }

    public void iniciarBatalha(String gameId) {
        Partida partida = buscarPartida(gameId);
        partida.iniciarBatalha();
    }

}

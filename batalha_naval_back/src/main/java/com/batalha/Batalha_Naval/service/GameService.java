package com.batalha.Batalha_Naval.service;

import com.batalha.Batalha_Naval.dominio.*;
import com.batalha.Batalha_Naval.dto.SalaResponse;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class GameService {

    private final Map<String, Partida> partidas = new ConcurrentHashMap<>();

    public Partida buscarPartida(String gameId) {
        Partida partida = partidas.get(gameId);
        if (partida == null) {
            throw new IllegalArgumentException("Partida não encontrada: " + gameId);
        }
        return partida;
    }

    public String criarPartida(String jogador1, String nome, String senha) {
        String id = UUID.randomUUID().toString();
        Partida partida = new Partida(jogador1, nome, senha);
        partidas.put(id, partida);
        return id;
    }

    public Partida entrarNaPartida(String gameId, String jogador2, String senha) {
        Partida partida = buscarPartida(gameId);
        partida.entrar(jogador2, senha);
        return partida;
    }

    public List<SalaResponse> listarPartidasAbertas() {
        List<SalaResponse> abertas = new ArrayList<>();
        for (String id : partidas.keySet()) {
            Partida p = partidas.get(id);
            if (p.getStatus() == StatusPartida.AGUARDANDO) {
                boolean temSenha = p.getSenha() != null && !p.getSenha().isBlank();
                abertas.add(new SalaResponse(id, p.getNome(), temSenha));
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

    public Partida marcarPronto(String gameId, String jogador) {
        Partida partida = buscarPartida(gameId);
        partida.marcarPronto(jogador);
        return partida;
    }

    public void iniciarBatalha(String gameId) {
        Partida partida = buscarPartida(gameId);
        partida.iniciarBatalha();
    }

    public void sairDaPartida(String gameId, String jogador) {
        Partida partida = partidas.get(gameId);
        if (partida == null) return;
        if (jogador.equals(partida.getJogador1())) {
            partidas.remove(gameId);
        } else if (jogador.equals(partida.getJogador2())) {
            partida.removerJogador2();
        }
    }
}

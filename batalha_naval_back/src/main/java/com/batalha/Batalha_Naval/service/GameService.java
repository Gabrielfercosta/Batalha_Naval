package com.batalha.Batalha_Naval.service;

import com.batalha.Batalha_Naval.dominio.*;
import org.springframework.stereotype.Service;

@Service
public class GameService extends ServicoPartidaBase<Partida> {

    @Override
    protected Partida novaPartida(String jogador, String nome, String senha) {
        return new Partida(jogador, nome, senha);
    }

    public ResultadoTiro atirar(String gameId, String jogador, Coordenada tiro) {
        return buscarPartida(gameId).atirar(jogador, tiro);
    }

    public void posicionarNavio(String gameId, String jogador, TipoNavio tipo, int linha, int coluna, Direcao direcao) {
        Partida partida = buscarPartida(gameId);
        Navio navio = FabricaNavio.criar(tipo, linha, coluna, direcao);
        Tabuleiro tabuleiro = jogador.equals(partida.getJogador1()) ? partida.getTabuleiro1() : partida.getTabuleiro2();
        tabuleiro.posicionarNavio(navio);
    }
}

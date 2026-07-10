package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.dominio.Direcao;
import com.batalha.Batalha_Naval.service.ServicoPartidaBase;
import org.springframework.stereotype.Service;

@Service
public class MinadoService extends ServicoPartidaBase<PartidaMinada> {

    @Override
    protected PartidaMinada novaPartida(String jogador, String nome, String senha) {
        return new PartidaMinada(jogador, nome, senha);
    }

    public void posicionarNavio(String gameId, String jogador, int linha, int coluna, int tamanho, Direcao direcao) {
        buscarPartida(gameId).posicionarNavio(jogador, linha, coluna, tamanho, direcao);
    }

    public void posicionarMina(String gameId, String jogador, int linha, int coluna) {
        buscarPartida(gameId).posicionarMina(jogador, linha, coluna);
    }

    public ResultadoTiroMinado atirar(String gameId, String jogador, int linha, int coluna) {
        return buscarPartida(gameId).atirar(jogador, linha, coluna);
    }
}

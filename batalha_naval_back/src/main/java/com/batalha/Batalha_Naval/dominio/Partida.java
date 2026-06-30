package com.batalha.Batalha_Naval.dominio;

import lombok.Getter;

@Getter
public class Partida {

    private final String jogador1;
    private String jogador2;

    private final Tabuleiro tabuleiro1 = new Tabuleiro();
    private final Tabuleiro tabuleiro2 = new Tabuleiro();

    private String turnoAtual;
    private StatusPartida status;
    private String vencedor;

    public Partida(String jogador1) {
        this.jogador1 = jogador1;
        this.jogador2 = null;
        this.turnoAtual = jogador1;
        this.status = StatusPartida.AGUARDANDO;
        this.vencedor = null;
    }

    public void entrar(String jogador2) {
        if (status != StatusPartida.AGUARDANDO) {
            throw new IllegalStateException("A partida não está aguardando jogador.");
        }
        this.jogador2 = jogador2;
        this.status = StatusPartida.POSICIONANDO;
    }

    public void iniciarBatalha() {
        if (status != StatusPartida.POSICIONANDO) {
            throw new IllegalStateException("A partida não está na fase de posicionamento.");
        }
        this.status = StatusPartida.EM_ANDAMENTO;
    }

    public ResultadoTiro atirar(String jogador, Coordenada tiro) {
        if (status != StatusPartida.EM_ANDAMENTO) {
            throw new IllegalStateException("A partida não está em andamento.");
        }

        if (!jogador.equals(turnoAtual)) {
            throw new IllegalStateException("Não é a vez do jogador " + jogador);
        }

        Tabuleiro tabuleiroOponente = jogador.equals(jogador1) ? tabuleiro2 : tabuleiro1;

        ResultadoTiro resultado = tabuleiroOponente.receberTiro(tiro);

        if (tabuleiroOponente.todosAfundados()) {
            status = StatusPartida.FINALIZADA;
            vencedor = jogador;
        } else {
            turnoAtual = jogador.equals(jogador1) ? jogador2 : jogador1;
        }

        return resultado;
    }
}

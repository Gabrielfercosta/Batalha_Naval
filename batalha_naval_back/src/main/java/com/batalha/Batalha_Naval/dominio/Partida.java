package com.batalha.Batalha_Naval.dominio;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class Partida {
    private final String jogador1;
    private String jogador2;
    private final Tabuleiro tabuleiro1 = new Tabuleiro();
    private final Tabuleiro tabuleiro2 = new Tabuleiro();
    private String turnoAtual;
    private StatusPartida status;
    private String vencedor;
    private final Set<String> prontos = new HashSet<>();
    private final String nome;
    private final String senha;

    public Partida(String jogador1, String nome, String senha) {
        this.jogador1 = jogador1;
        this.nome = nome;
        this.senha = senha;
        this.jogador2 = null;
        this.turnoAtual = jogador1;
        this.status = StatusPartida.AGUARDANDO;
        this.vencedor = null;
    }

    public void entrar(String jogador2, String senha) {
        if (status != StatusPartida.AGUARDANDO) {
            throw new IllegalStateException("A partida não está aguardando jogador.");
        }
        if (jogador2.equals(this.jogador1)) {
            throw new IllegalStateException("Você não pode entrar na sua própria sala.");
        }
        if (this.senha != null && !this.senha.isBlank() && !this.senha.equals(senha)) {
            throw new IllegalArgumentException("Senha da sala incorreta.");
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

    public void marcarPronto(String jogador) {
        if (status == StatusPartida.EM_ANDAMENTO || status == StatusPartida.FINALIZADA) {
            throw new IllegalStateException("A partida já começou ou terminou.");
        }
        prontos.add(jogador);
        if (jogador2 != null && prontos.contains(jogador1) && prontos.contains(jogador2)) {
            status = StatusPartida.EM_ANDAMENTO;
        }
    }

    public void removerJogador2() {
        this.jogador2 = null;
        this.prontos.clear();
        this.tabuleiro2.limpar();
        if (this.status == StatusPartida.POSICIONANDO) {
            this.status = StatusPartida.AGUARDANDO;
            this.turnoAtual = this.jogador1;
        }
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
        } else if (resultado == ResultadoTiro.AGUA) {
            turnoAtual = jogador.equals(jogador1) ? jogador2 : jogador1;
        }

        return resultado;
    }

}

package com.batalha.Batalha_Naval.dominio;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public abstract class PartidaBase {

    protected final String jogador1;
    protected String jogador2;
    protected final String nome;
    protected final String senha;
    protected String turnoAtual;
    protected StatusPartida status;
    protected String vencedor;
    protected final Set<String> prontos = new HashSet<>();
    protected final long criadaEm = System.currentTimeMillis();

    protected PartidaBase(String jogador1, String nome, String senha) {
        this.jogador1 = jogador1;
        this.nome = nome;
        this.senha = senha;
        this.turnoAtual = jogador1;
        this.status = StatusPartida.AGUARDANDO;
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

    public void abandonar(String jogador) {
        if (status == StatusPartida.EM_ANDAMENTO) {
            vencedor = oponente(jogador);
            status = StatusPartida.FINALIZADA;
        }
    }

    public abstract void marcarPronto(String jogador);

    public abstract void removerJogador2();

    protected boolean ehJogador1(String jogador) {
        return jogador.equals(jogador1);
    }

    protected String oponente(String jogador) {
        return ehJogador1(jogador) ? jogador2 : jogador1;
    }

    protected boolean ambosProntos() {
        return jogador2 != null && prontos.contains(jogador1) && prontos.contains(jogador2);
    }

    protected void garantirNaoIniciada() {
        if (status == StatusPartida.EM_ANDAMENTO || status == StatusPartida.FINALIZADA) {
            throw new IllegalStateException("A partida já começou ou terminou.");
        }
    }
}

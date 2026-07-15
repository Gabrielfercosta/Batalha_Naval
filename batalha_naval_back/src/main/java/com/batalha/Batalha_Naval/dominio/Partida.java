package com.batalha.Batalha_Naval.dominio;

import lombok.Getter;

@Getter
public class Partida extends PartidaBase {

    public static final int TOTAL_NAVIOS = 5;

    private final Tabuleiro tabuleiro1 = new Tabuleiro();
    private final Tabuleiro tabuleiro2 = new Tabuleiro();

    public Partida(String jogador1, String nome, String senha) {
        super(jogador1, nome, senha);
    }

    @Override
    public void marcarPronto(String jogador) {
        garantirNaoIniciada();
        Tabuleiro tab = ehJogador1(jogador) ? tabuleiro1 : tabuleiro2;
        if (tab.getNavios().size() < TOTAL_NAVIOS) {
            throw new IllegalStateException("Posicione todos os navios antes de ficar pronto.");
        }
        prontos.add(jogador);
        if (ambosProntos()) {
            status = StatusPartida.EM_ANDAMENTO;
        }
    }

    @Override
    public void removerJogador2() {
        super.removerJogador2();
        this.tabuleiro2.limpar();
    }

    public Navio navioAfundadoEm(String jogador, Coordenada tiro) {
        Tabuleiro alvo = ehJogador1(jogador) ? tabuleiro2 : tabuleiro1;
        return alvo.navioEm(tiro);
    }

    public ResultadoTiro atirar(String jogador, Coordenada tiro) {
        if (status != StatusPartida.EM_ANDAMENTO) {
            throw new IllegalStateException("A partida não está em andamento.");
        }
        if (!jogador.equals(turnoAtual)) {
            throw new IllegalStateException("Não é a vez do jogador " + jogador);
        }
        Tabuleiro tabuleiroOponente = ehJogador1(jogador) ? tabuleiro2 : tabuleiro1;
        ResultadoTiro resultado = tabuleiroOponente.receberTiro(tiro);
        if (tabuleiroOponente.todosAfundados()) {
            status = StatusPartida.FINALIZADA;
            vencedor = jogador;
        } else if (resultado == ResultadoTiro.AGUA) {
            turnoAtual = oponente(jogador);
        }
        return resultado;
    }
}

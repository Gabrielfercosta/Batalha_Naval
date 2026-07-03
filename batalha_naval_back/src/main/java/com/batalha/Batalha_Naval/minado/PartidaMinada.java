package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.dominio.Direcao;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class PartidaMinada {

    public static final int[] TAMANHOS_NAVIOS = {5, 4, 3, 3, 2};
    public static final int QTD_MINAS = 20;

    private final String jogador1;
    private String jogador2;
    private final String nome;
    private final String senha;

    private final TabuleiroMinado tabuleiro1 = new TabuleiroMinado();
    private final TabuleiroMinado tabuleiro2 = new TabuleiroMinado();

    private String turnoAtual;
    private StatusPartidaMinada status;
    private String vencedor;

    private final Set<String> prontos = new HashSet<>();
    private final Set<String> jaAtirou = new HashSet<>();

    public PartidaMinada(String jogador1, String nome, String senha) {
        this.jogador1 = jogador1;
        this.nome = nome;
        this.senha = senha;
        this.jogador2 = null;
        this.turnoAtual = jogador1;
        this.status = StatusPartidaMinada.AGUARDANDO;
        this.vencedor = null;
    }

    public void entrar(String jogador2, String senha) {
        if (status != StatusPartidaMinada.AGUARDANDO) {
            throw new IllegalStateException("A partida não está aguardando jogador.");
        }
        if (jogador2.equals(this.jogador1)) {
            throw new IllegalStateException("Você não pode entrar na sua própria sala.");
        }
        if (this.senha != null && !this.senha.isBlank() && !this.senha.equals(senha)) {
            throw new IllegalArgumentException("Senha da sala incorreta.");
        }
        this.jogador2 = jogador2;
        this.status = StatusPartidaMinada.POSICIONANDO;
    }

    public void posicionarNavio(String jogador, int linha, int coluna, int tamanho, Direcao direcao) {
        tabuleiroDoJogador(jogador).posicionarNavio(linha, coluna, tamanho, direcao);
    }

    public void posicionarMina(String jogador, int linha, int coluna) {
        tabuleiroDoJogador(jogador).posicionarMina(linha, coluna);
    }

    public void marcarPronto(String jogador) {
        if (status == StatusPartidaMinada.EM_ANDAMENTO || status == StatusPartidaMinada.FINALIZADA) {
            throw new IllegalStateException("A partida já começou ou terminou.");
        }

        TabuleiroMinado tab = tabuleiroDoJogador(jogador);

        int totalNavios = 0;
        for (int t : TAMANHOS_NAVIOS) totalNavios += t;

        if (tab.contarNavios() < totalNavios) {
            throw new IllegalStateException("Posicione todos os navios antes de ficar pronto.");
        }
        if (tab.contarMinas() < QTD_MINAS) {
            throw new IllegalStateException("Posicione todas as minas antes de ficar pronto.");
        }

        prontos.add(jogador);
        if (jogador2 != null && prontos.contains(jogador1) && prontos.contains(jogador2)) {
            status = StatusPartidaMinada.EM_ANDAMENTO;
        }
    }

    public void removerJogador2() {
        this.jogador2 = null;
        this.prontos.clear();
        this.jaAtirou.clear();
        this.tabuleiro2.limpar();
        if (this.status == StatusPartidaMinada.POSICIONANDO) {
            this.status = StatusPartidaMinada.AGUARDANDO;
            this.turnoAtual = this.jogador1;
        }
    }

    public ResultadoTiroMinado atirar(String jogador, int linha, int coluna) {
        if (status != StatusPartidaMinada.EM_ANDAMENTO) {
            throw new IllegalStateException("A partida não está em andamento.");
        }

        TabuleiroMinado oponente = jogador.equals(jogador1) ? tabuleiro2 : tabuleiro1;

        boolean tiroSeguro = !jaAtirou.contains(jogador);
        jaAtirou.add(jogador);

        ResultadoTiroMinado resultado = oponente.receberTiro(linha, coluna, tiroSeguro);

        if (resultado == ResultadoTiroMinado.MINA) {
            status = StatusPartidaMinada.FINALIZADA;
            vencedor = jogador.equals(jogador1) ? jogador2 : jogador1;
        } else if (resultado == ResultadoTiroMinado.NAVIO && oponente.todosNaviosRevelados()) {
            status = StatusPartidaMinada.FINALIZADA;
            vencedor = jogador;
        }

        return resultado;
    }

    private TabuleiroMinado tabuleiroDoJogador(String jogador) {
        return jogador.equals(jogador1) ? tabuleiro1 : tabuleiro2;
    }
}

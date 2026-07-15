package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.dominio.Direcao;
import com.batalha.Batalha_Naval.dominio.PartidaBase;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class PartidaMinada extends PartidaBase {

    public static final int[] TAMANHOS_NAVIOS = {5, 4, 3, 3, 2};
    public static final int QTD_MINAS = 20;

    private final TabuleiroMinado tabuleiro1 = new TabuleiroMinado();
    private final TabuleiroMinado tabuleiro2 = new TabuleiroMinado();
    private final Set<String> jaAtirou = new HashSet<>();
    private final Set<String> naBatalha = new HashSet<>();
    private boolean contagemIniciada = false;

    public PartidaMinada(String jogador1, String nome, String senha) {
        super(jogador1, nome, senha);
    }

    public boolean registrarChegada(String jogador) {
        naBatalha.add(jogador);
        if (!contagemIniciada && jogador2 != null
                && naBatalha.contains(jogador1) && naBatalha.contains(jogador2)) {
            contagemIniciada = true;
            return true;
        }
        return false;
    }

    public void posicionarNavio(String jogador, int linha, int coluna, int tamanho, Direcao direcao) {
        tabuleiroDoJogador(jogador).posicionarNavio(linha, coluna, tamanho, direcao);
    }

    public void posicionarMina(String jogador, int linha, int coluna) {
        tabuleiroDoJogador(jogador).posicionarMina(linha, coluna);
    }

    @Override
    public void marcarPronto(String jogador) {
        garantirNaoIniciada();
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
        if (ambosProntos()) {
            status = StatusPartida.EM_ANDAMENTO;
        }
    }

    @Override
    public void removerJogador2() {
        super.removerJogador2();
        this.tabuleiro2.limpar();
    }

    public ResultadoTiroMinado atirar(String jogador, int linha, int coluna) {
        if (status != StatusPartida.EM_ANDAMENTO) {
            throw new IllegalStateException("A partida não está em andamento.");
        }

        TabuleiroMinado tabuleiroOponente = ehJogador1(jogador) ? tabuleiro2 : tabuleiro1;

        boolean tiroSeguro = !jaAtirou.contains(jogador);
        jaAtirou.add(jogador);

        ResultadoTiroMinado resultado = tabuleiroOponente.receberTiro(linha, coluna, tiroSeguro);

        if (resultado == ResultadoTiroMinado.MINA) {
            status = StatusPartida.FINALIZADA;
            vencedor = oponente(jogador);
        } else if (resultado == ResultadoTiroMinado.NAVIO && tabuleiroOponente.todosNaviosRevelados()) {
            status = StatusPartida.FINALIZADA;
            vencedor = jogador;
        }

        return resultado;
    }

    private TabuleiroMinado tabuleiroDoJogador(String jogador) {
        return ehJogador1(jogador) ? tabuleiro1 : tabuleiro2;
    }
}

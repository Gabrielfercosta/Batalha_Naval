package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.dominio.Direcao;

import java.util.ArrayList;
import java.util.List;

public class TabuleiroMinado {

    public static final int TAMANHO = 16;

    private final EstadoCasa[][] grade = new EstadoCasa[TAMANHO][TAMANHO];
    private final boolean[][] revelada = new boolean[TAMANHO][TAMANHO];

    public TabuleiroMinado() {
        for (int l = 0; l < TAMANHO; l++) {
            for (int c = 0; c < TAMANHO; c++) {
                grade[l][c] = EstadoCasa.AGUA;
            }
        }
    }

    public int contarNavios() {
        int total = 0;
        for (int l = 0; l < TAMANHO; l++) {
            for (int c = 0; c < TAMANHO; c++) {
                if (grade[l][c] == EstadoCasa.NAVIO) total++;
            }
        }
        return total;
    }

    public int contarMinas() {
        int total = 0;
        for (int l = 0; l < TAMANHO; l++) {
            for (int c = 0; c < TAMANHO; c++) {
                if (grade[l][c] == EstadoCasa.MINA) total++;
            }
        }
        return total;
    }

    public void posicionarNavio(int linhaInicial, int colunaInicial, int tamanho, Direcao direcao) {
        if (!cabeNavio(linhaInicial, colunaInicial, tamanho, direcao)) {
            throw new IllegalArgumentException("Navio não cabe nessa posição.");
        }
        for (int i = 0; i < tamanho; i++) {
            int l = direcao == Direcao.HORIZONTAL ? linhaInicial : linhaInicial + i;
            int c = direcao == Direcao.HORIZONTAL ? colunaInicial + i : colunaInicial;
            grade[l][c] = EstadoCasa.NAVIO;
        }
    }

    private boolean cabeNavio(int linha, int coluna, int tamanho, Direcao direcao) {
        for (int i = 0; i < tamanho; i++) {
            int l = direcao == Direcao.HORIZONTAL ? linha : linha + i;
            int c = direcao == Direcao.HORIZONTAL ? coluna + i : coluna;
            if (!dentro(l, c)) return false;
            if (grade[l][c] != EstadoCasa.AGUA) return false;
        }
        return true;
    }

    public void posicionarMina(int linha, int coluna) {
        if (!dentro(linha, coluna)) {
            throw new IllegalArgumentException("Posição fora do tabuleiro.");
        }
        if (grade[linha][coluna] != EstadoCasa.AGUA) {
            throw new IllegalArgumentException("Só dá pra colocar mina na água.");
        }
        grade[linha][coluna] = EstadoCasa.MINA;
    }

    public ResultadoTiroMinado receberTiro(int linha, int coluna, boolean tiroSeguro) {
        if (!dentro(linha, coluna)) {
            throw new IllegalArgumentException("Posição fora do tabuleiro.");
        }
        if (revelada[linha][coluna]) {
            throw new IllegalArgumentException("Essa casa já foi atacada.");
        }

        if (grade[linha][coluna] == EstadoCasa.MINA) {
            if (tiroSeguro) {
                grade[linha][coluna] = EstadoCasa.AGUA;
                abrirCascataForcada(linha, coluna);
                return ResultadoTiroMinado.AGUA;
            }
            revelada[linha][coluna] = true;
            return ResultadoTiroMinado.MINA;
        }

        if (grade[linha][coluna] == EstadoCasa.NAVIO) {
            revelada[linha][coluna] = true;
            return ResultadoTiroMinado.NAVIO;
        }

        if (tiroSeguro) {
            abrirCascataForcada(linha, coluna);
        } else {
            abrirCascata(linha, coluna);
        }
        return ResultadoTiroMinado.AGUA;
    }

    private void abrirCascata(int linha, int coluna) {
        if (!dentro(linha, coluna)) return;
        if (revelada[linha][coluna]) return;
        if (grade[linha][coluna] != EstadoCasa.AGUA) return;

        revelada[linha][coluna] = true;

        Pista pista = contarVizinhos(linha, coluna);
        if (pista.getMinas() > 0 || pista.getNavios() > 0) return;

        for (int dl = -1; dl <= 1; dl++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dl == 0 && dc == 0) continue;
                abrirCascata(linha + dl, coluna + dc);
            }
        }
    }

    public void limpar() {
        for (int l = 0; l < TAMANHO; l++) {
            for (int c = 0; c < TAMANHO; c++) {
                grade[l][c] = EstadoCasa.AGUA;
                revelada[l][c] = false;
            }
        }
    }

    private void abrirCascataForcada(int linha, int coluna) {
        if (!dentro(linha, coluna)) return;
        if (revelada[linha][coluna]) return;
        if (grade[linha][coluna] != EstadoCasa.AGUA) return;

        revelada[linha][coluna] = true;

        for (int dl = -1; dl <= 1; dl++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dl == 0 && dc == 0) continue;
                int nl = linha + dl;
                int nc = coluna + dc;
                abrirCascata(nl, nc);
            }
        }
    }

    public Pista contarVizinhos(int linha, int coluna) {
        int minas = 0;
        int navios = 0;
        for (int dl = -1; dl <= 1; dl++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dl == 0 && dc == 0) continue;
                int l = linha + dl;
                int c = coluna + dc;
                if (dentro(l, c)) {
                    if (grade[l][c] == EstadoCasa.MINA) minas++;
                    if (grade[l][c] == EstadoCasa.NAVIO) navios++;
                }
            }
        }
        return new Pista(minas, navios);
    }

    public boolean todosNaviosRevelados() {
        for (int l = 0; l < TAMANHO; l++) {
            for (int c = 0; c < TAMANHO; c++) {
                if (grade[l][c] == EstadoCasa.NAVIO && !revelada[l][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<int[]> casasReveladas() {
        List<int[]> lista = new ArrayList<>();
        for (int l = 0; l < TAMANHO; l++) {
            for (int c = 0; c < TAMANHO; c++) {
                if (revelada[l][c]) lista.add(new int[]{l, c});
            }
        }
        return lista;
    }

    private boolean dentro(int l, int c) {
        return l >= 0 && l < TAMANHO && c >= 0 && c < TAMANHO;
    }

    public EstadoCasa getEstado(int l, int c) {
        return grade[l][c];
    }

    public boolean isRevelada(int l, int c) {
        return revelada[l][c];
    }
}

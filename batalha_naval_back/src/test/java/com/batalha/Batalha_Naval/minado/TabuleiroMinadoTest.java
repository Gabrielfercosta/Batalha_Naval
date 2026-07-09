package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.dominio.Direcao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TabuleiroMinadoTest {

    private TabuleiroMinado tabuleiro;

    @BeforeEach
    void setUp() {
        tabuleiro = new TabuleiroMinado();
    }

    @Test
    void tabuleiroComecaVazio() {
        assertEquals(0, tabuleiro.contarNavios());
        assertEquals(0, tabuleiro.contarMinas());
    }

    @Test
    void posicionarNavioHorizontal() {
        tabuleiro.posicionarNavio(0, 0, 5, Direcao.HORIZONTAL);
        assertEquals(5, tabuleiro.contarNavios());
    }

    @Test
    void posicionarNavioVertical() {
        tabuleiro.posicionarNavio(0, 0, 3, Direcao.VERTICAL);
        assertEquals(3, tabuleiro.contarNavios());
    }

    @Test
    void navioForaDoTabuleiroLancaErro() {
        assertThrows(IllegalArgumentException.class, () ->
                tabuleiro.posicionarNavio(0, 14, 5, Direcao.HORIZONTAL));
    }

    @Test
    void navioSobreOutroNavioLancaErro() {
        tabuleiro.posicionarNavio(0, 0, 5, Direcao.HORIZONTAL);
        assertThrows(IllegalArgumentException.class, () ->
                tabuleiro.posicionarNavio(0, 2, 3, Direcao.HORIZONTAL));
    }

    @Test
    void posicionarMina() {
        tabuleiro.posicionarMina(5, 5);
        assertEquals(1, tabuleiro.contarMinas());
        assertEquals(EstadoCasa.MINA, tabuleiro.getEstado(5, 5));
    }

    @Test
    void minaForaDoTabuleiroLancaErro() {
        assertThrows(IllegalArgumentException.class, () ->
                tabuleiro.posicionarMina(20, 20));
    }

    @Test
    void minaSobreNavioLancaErro() {
        tabuleiro.posicionarNavio(0, 0, 3, Direcao.HORIZONTAL);
        assertThrows(IllegalArgumentException.class, () ->
                tabuleiro.posicionarMina(0, 1));
    }

    @Test
    void tiroEmAguaRetornaAgua() {
        ResultadoTiroMinado resultado = tabuleiro.receberTiro(5, 5, false);
        assertEquals(ResultadoTiroMinado.AGUA, resultado);
    }

    @Test
    void tiroEmNavioRetornaNavio() {
        tabuleiro.posicionarNavio(3, 3, 2, Direcao.HORIZONTAL);
        ResultadoTiroMinado resultado = tabuleiro.receberTiro(3, 3, false);
        assertEquals(ResultadoTiroMinado.NAVIO, resultado);
    }

    @Test
    void tiroEmMinaRetornaMina() {
        tabuleiro.posicionarMina(7, 7);
        ResultadoTiroMinado resultado = tabuleiro.receberTiro(7, 7, false);
        assertEquals(ResultadoTiroMinado.MINA, resultado);
    }

    @Test
    void tiroSeguroEmMinaRetornaAgua() {
        tabuleiro.posicionarMina(7, 7);
        ResultadoTiroMinado resultado = tabuleiro.receberTiro(7, 7, true);
        assertEquals(ResultadoTiroMinado.AGUA, resultado);
    }

    @Test
    void tiroRepetidoLancaErro() {
        tabuleiro.receberTiro(5, 5, false);
        assertThrows(IllegalArgumentException.class, () ->
                tabuleiro.receberTiro(5, 5, false));
    }

    @Test
    void todosNaviosReveladosQuandoTodosAcertados() {
        tabuleiro.posicionarNavio(0, 0, 2, Direcao.HORIZONTAL);
        assertFalse(tabuleiro.todosNaviosRevelados());

        tabuleiro.receberTiro(0, 0, false);
        tabuleiro.receberTiro(0, 1, false);
        assertTrue(tabuleiro.todosNaviosRevelados());
    }

    @Test
    void contarVizinhosComMinaENavioAoRedor() {
        tabuleiro.posicionarMina(4, 4);
        tabuleiro.posicionarNavio(4, 6, 2, Direcao.HORIZONTAL);

        Pista pista = tabuleiro.contarVizinhos(4, 5);
        assertEquals(1, pista.getMinas());
        assertEquals(1, pista.getNavios());
    }

    @Test
    void limparResetaTudo() {
        tabuleiro.posicionarNavio(0, 0, 3, Direcao.HORIZONTAL);
        tabuleiro.posicionarMina(5, 5);
        tabuleiro.receberTiro(0, 0, false);

        tabuleiro.limpar();

        assertEquals(0, tabuleiro.contarNavios());
        assertEquals(0, tabuleiro.contarMinas());
        assertFalse(tabuleiro.isRevelada(0, 0));
    }
}

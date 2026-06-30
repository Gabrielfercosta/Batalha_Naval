package com.batalha.Batalha_Naval.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabuleiroTest {

    @Test
    void tiroNaAguaQuandoNaoTemNavio() {
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.posicionarNavio(new Navio(TipoNavio.DESTROYER, List.of(
                new Coordenada(0, 0),
                new Coordenada(0, 1)
        )));

        ResultadoTiro resultado = tabuleiro.receberTiro(new Coordenada(5, 5));

        assertEquals(ResultadoTiro.AGUA, resultado);
    }

    @Test
    void tiroAcertaNavio() {
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.posicionarNavio(new Navio(TipoNavio.DESTROYER, List.of(
                new Coordenada(0, 0),
                new Coordenada(0, 1)
        )));

        ResultadoTiro resultado = tabuleiro.receberTiro(new Coordenada(0, 0));

        assertEquals(ResultadoTiro.ACERTO, resultado);
    }

    @Test
    void tiroAfundaNavio() {
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.posicionarNavio(new Navio(TipoNavio.DESTROYER, List.of(
                new Coordenada(0, 0),
                new Coordenada(0, 1)
        )));

        tabuleiro.receberTiro(new Coordenada(0, 0));
        ResultadoTiro resultado = tabuleiro.receberTiro(new Coordenada(0, 1));

        assertEquals(ResultadoTiro.AFUNDADO, resultado);
    }

    @Test
    void naoPermitePosicionarForaDoTabuleiro() {
        Tabuleiro tabuleiro = new Tabuleiro();

        assertThrows(IllegalArgumentException.class, () -> {
            tabuleiro.posicionarNavio(new Navio(TipoNavio.DESTROYER, List.of(
                    new Coordenada(0, 0),
                    new Coordenada(0, 99)
            )));
        });
    }

    @Test
    void naoPermiteSobreporNavios() {
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.posicionarNavio(new Navio(TipoNavio.DESTROYER, List.of(
                new Coordenada(0, 0),
                new Coordenada(0, 1)
        )));

        assertThrows(IllegalArgumentException.class, () -> {
            tabuleiro.posicionarNavio(new Navio(TipoNavio.SUBMARINO, List.of(
                    new Coordenada(0, 1),
                    new Coordenada(0, 2),
                    new Coordenada(0, 3)
            )));
        });
    }

    @Test
    void todosAfundadosQuandoUnicoNavioAfunda() {
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.posicionarNavio(new Navio(TipoNavio.DESTROYER, List.of(
                new Coordenada(0, 0),
                new Coordenada(0, 1)
        )));

        assertFalse(tabuleiro.todosAfundados());

        tabuleiro.receberTiro(new Coordenada(0, 0));
        tabuleiro.receberTiro(new Coordenada(0, 1));

        assertTrue(tabuleiro.todosAfundados());
    }
}

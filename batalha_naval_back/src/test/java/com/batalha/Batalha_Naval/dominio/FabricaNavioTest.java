package com.batalha.Batalha_Naval.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FabricaNavioTest {

    @Test
    void criaNavioHorizontalComPosicoesCorretas() {
        Navio navio = FabricaNavio.criar(TipoNavio.CRUZADOR, 2, 3, Direcao.HORIZONTAL);

        assertEquals(3, navio.getPosicoes().size());
        assertTrue(navio.ocupaPosicao(new Coordenada(2, 3)));
        assertTrue(navio.ocupaPosicao(new Coordenada(2, 4)));
        assertTrue(navio.ocupaPosicao(new Coordenada(2, 5)));
    }

    @Test
    void criaNavioVerticalComPosicoesCorretas() {
        Navio navio = FabricaNavio.criar(TipoNavio.CRUZADOR, 2, 3, Direcao.VERTICAL);

        assertEquals(3, navio.getPosicoes().size());
        assertTrue(navio.ocupaPosicao(new Coordenada(2, 3)));
        assertTrue(navio.ocupaPosicao(new Coordenada(3, 3)));
        assertTrue(navio.ocupaPosicao(new Coordenada(4, 3)));
    }

    @Test
    void portaAvioesTemTamanho5() {
        Navio navio = FabricaNavio.criar(TipoNavio.PORTA_AVIOES, 0, 0, Direcao.HORIZONTAL);
        assertEquals(5, navio.getPosicoes().size());
    }

    @Test
    void destroyerTemTamanho2() {
        Navio navio = FabricaNavio.criar(TipoNavio.DESTROYER, 0, 0, Direcao.VERTICAL);
        assertEquals(2, navio.getPosicoes().size());
    }
}

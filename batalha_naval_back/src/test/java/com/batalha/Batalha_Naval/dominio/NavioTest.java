package com.batalha.Batalha_Naval.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavioTest {

    @Test
    void naviorNaoComecaAfundado() {
        Navio navio = new Navio(TipoNavio.DESTROYER, List.of(
                new Coordenada(0, 0),
                new Coordenada(0, 1)
        ));

        assertFalse(navio.estaAfundado());
    }

    @Test
    void navioAfundaQuandoTodasPosicoesSaoAtingidas() {
        Navio navio = new Navio(TipoNavio.DESTROYER, List.of(
                new Coordenada(0, 0),
                new Coordenada(0, 1)
        ));

        navio.registrarTiro(new Coordenada(0, 0));
        assertFalse(navio.estaAfundado());

        navio.registrarTiro(new Coordenada(0, 1));
        assertTrue(navio.estaAfundado());
    }

    @Test
    void ocupaPosicaoReconheceCasaDoNavio() {
        Navio navio = new Navio(TipoNavio.DESTROYER, List.of(
                new Coordenada(0, 0),
                new Coordenada(0, 1)
        ));

        assertTrue(navio.ocupaPosicao(new Coordenada(0, 0)));
        assertFalse(navio.ocupaPosicao(new Coordenada(5, 5)));
    }
}

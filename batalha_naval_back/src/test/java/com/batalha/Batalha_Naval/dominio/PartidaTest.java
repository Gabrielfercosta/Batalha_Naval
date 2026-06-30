package com.batalha.Batalha_Naval.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartidaTest {

    private Partida criarPartidaComNavios() {
        Partida partida = new Partida("ana");

        partida.getTabuleiro1().posicionarNavio(new Navio(TipoNavio.DESTROYER, List.of(
                new Coordenada(0, 0),
                new Coordenada(0, 1)
        )));

        partida.getTabuleiro2().posicionarNavio(new Navio(TipoNavio.DESTROYER, List.of(
                new Coordenada(5, 5),
                new Coordenada(5, 6)
        )));

        return partida;
    }

    @Test
    void jogador1ComecaJogando() {
        Partida partida = criarPartidaComNavios();
        assertEquals("ana", partida.getTurnoAtual());
    }

    @Test
    void naoPermiteAtirarAntesDeIniciar() {
        Partida partida = criarPartidaComNavios();

        assertThrows(IllegalStateException.class, () -> {
            partida.atirar("ana", new Coordenada(5, 5));
        });
    }

    @Test
    void naoPermiteAtirarForaDoTurno() {
        Partida partida = criarPartidaComNavios();
        partida.iniciarBatalha();

        assertThrows(IllegalStateException.class, () -> {
            partida.atirar("bob", new Coordenada(0, 0));
        });
    }

    @Test
    void turnoAlternaDepoisDoTiro() {
        Partida partida = criarPartidaComNavios();
        partida.iniciarBatalha();

        partida.atirar("ana", new Coordenada(9, 9));

        assertEquals("bob", partida.getTurnoAtual());
    }

    @Test
    void tiroAcertaNoTabuleiroDoOponente() {
        Partida partida = criarPartidaComNavios();
        partida.iniciarBatalha();

        ResultadoTiro resultado = partida.atirar("ana", new Coordenada(5, 5));

        assertEquals(ResultadoTiro.ACERTO, resultado);
    }

    @Test
    void jogadorVenceAoAfundarTodosOsNaviosDoOponente() {
        Partida partida = criarPartidaComNavios();
        partida.iniciarBatalha();

        partida.atirar("ana", new Coordenada(5, 5));
        partida.atirar("bob", new Coordenada(9, 9));
        ResultadoTiro resultado = partida.atirar("ana", new Coordenada(5, 6));

        assertEquals(ResultadoTiro.AFUNDADO, resultado);
        assertEquals(StatusPartida.FINALIZADA, partida.getStatus());
        assertEquals("ana", partida.getVencedor());
    }
}

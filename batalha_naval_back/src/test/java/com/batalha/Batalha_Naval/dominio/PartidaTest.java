package com.batalha.Batalha_Naval.dominio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PartidaTest {

    private Partida partida;

    @BeforeEach
    void setUp() {
        partida = new Partida("Alice", "Sala1", null);
    }

    @Test
    void partidaCriadaComStatusAguardando() {
        assertEquals(StatusPartida.AGUARDANDO, partida.getStatus());
        assertEquals("Alice", partida.getJogador1());
        assertNull(partida.getJogador2());
    }

    @Test
    void jogador2EntraEStatusViraPosicionando() {
        partida.entrar("Bob", null);
        assertEquals("Bob", partida.getJogador2());
        assertEquals(StatusPartida.POSICIONANDO, partida.getStatus());
    }

    @Test
    void naoPermiteEntrarNaPropriaSala() {
        assertThrows(IllegalStateException.class, () -> partida.entrar("Alice", null));
    }

    @Test
    void naoPermiteEntrarComSenhaErrada() {
        Partida comSenha = new Partida("Alice", "Sala", "1234");
        assertThrows(IllegalArgumentException.class, () -> comSenha.entrar("Bob", "errada"));
    }

    @Test
    void permiteEntrarComSenhaCorreta() {
        Partida comSenha = new Partida("Alice", "Sala", "1234");
        comSenha.entrar("Bob", "1234");
        assertEquals("Bob", comSenha.getJogador2());
    }

    @Test
    void naoPermiteEntrarEmPartidaQueNaoEstaAguardando() {
        partida.entrar("Bob", null);
        assertThrows(IllegalStateException.class, () -> partida.entrar("Carlos", null));
    }

    @Test
    void marcarProntoSemNaviosSuficientesLancaErro() {
        partida.entrar("Bob", null);
        assertThrows(IllegalStateException.class, () -> partida.marcarPronto("Alice"));
    }

    @Test
    void marcarProntoComNaviosSuficientesIniciaBatalha() {
        partida.entrar("Bob", null);
        posicionarFrotaCompleta(partida.getTabuleiro1());
        posicionarFrotaCompleta(partida.getTabuleiro2());

        partida.marcarPronto("Alice");
        assertEquals(StatusPartida.POSICIONANDO, partida.getStatus());

        partida.marcarPronto("Bob");
        assertEquals(StatusPartida.EM_ANDAMENTO, partida.getStatus());
    }

    @Test
    void atirarNaVezCertaFunciona() {
        iniciarPartida();
        ResultadoTiro resultado = partida.atirar("Alice", new Coordenada(5, 5));
        assertNotNull(resultado);
    }

    @Test
    void atirarForaDaVezLancaErro() {
        iniciarPartida();
        assertThrows(IllegalStateException.class, () -> partida.atirar("Bob", new Coordenada(0, 0)));
    }

    @Test
    void turnoPassaAoErrar() {
        iniciarPartida();
        partida.atirar("Alice", new Coordenada(9, 9));
        assertEquals("Bob", partida.getTurnoAtual());
    }

    @Test
    void turnoNaoPassaAoAcertar() {
        iniciarPartida();
        partida.atirar("Alice", new Coordenada(0, 0));
        assertEquals("Alice", partida.getTurnoAtual());
    }

    @Test
    void abandonarDaVitoriaAoOutro() {
        iniciarPartida();
        partida.abandonar("Alice");
        assertEquals(StatusPartida.FINALIZADA, partida.getStatus());
        assertEquals("Bob", partida.getVencedor());
    }

    @Test
    void removerJogador2VoltaParaAguardando() {
        partida.entrar("Bob", null);
        partida.removerJogador2();
        assertNull(partida.getJogador2());
        assertEquals(StatusPartida.AGUARDANDO, partida.getStatus());
    }

    @Test
    void vencedorQuandoAfundaTodosOsNavios() {
        iniciarPartida();
        partida.atirar("Alice", new Coordenada(0, 0));
        partida.atirar("Alice", new Coordenada(0, 1));
        partida.atirar("Alice", new Coordenada(1, 0));
        partida.atirar("Alice", new Coordenada(1, 1));
        partida.atirar("Alice", new Coordenada(1, 2));
        partida.atirar("Alice", new Coordenada(2, 0));
        partida.atirar("Alice", new Coordenada(2, 1));
        partida.atirar("Alice", new Coordenada(2, 2));
        partida.atirar("Alice", new Coordenada(3, 0));
        partida.atirar("Alice", new Coordenada(3, 1));
        partida.atirar("Alice", new Coordenada(3, 2));
        partida.atirar("Alice", new Coordenada(3, 3));
        partida.atirar("Alice", new Coordenada(4, 0));
        partida.atirar("Alice", new Coordenada(4, 1));
        partida.atirar("Alice", new Coordenada(4, 2));
        partida.atirar("Alice", new Coordenada(4, 3));
        partida.atirar("Alice", new Coordenada(4, 4));
        assertEquals(StatusPartida.FINALIZADA, partida.getStatus());
        assertEquals("Alice", partida.getVencedor());
    }

    private void iniciarPartida() {
        partida.entrar("Bob", null);
        posicionarFrotaCompleta(partida.getTabuleiro1());

        Navio destroyer = new Navio(TipoNavio.DESTROYER, List.of(new Coordenada(0, 0), new Coordenada(0, 1)));
        Navio sub = new Navio(TipoNavio.SUBMARINO, List.of(new Coordenada(1, 0), new Coordenada(1, 1), new Coordenada(1, 2)));
        Navio cruzador = new Navio(TipoNavio.CRUZADOR, List.of(new Coordenada(2, 0), new Coordenada(2, 1), new Coordenada(2, 2)));
        Navio encouracado = new Navio(TipoNavio.ENCOURACADO, List.of(new Coordenada(3, 0), new Coordenada(3, 1), new Coordenada(3, 2), new Coordenada(3, 3)));
        Navio porta = new Navio(TipoNavio.PORTA_AVIOES, List.of(new Coordenada(4, 0), new Coordenada(4, 1), new Coordenada(4, 2), new Coordenada(4, 3), new Coordenada(4, 4)));
        partida.getTabuleiro2().posicionarNavio(destroyer);
        partida.getTabuleiro2().posicionarNavio(sub);
        partida.getTabuleiro2().posicionarNavio(cruzador);
        partida.getTabuleiro2().posicionarNavio(encouracado);
        partida.getTabuleiro2().posicionarNavio(porta);

        partida.marcarPronto("Alice");
        partida.marcarPronto("Bob");
    }

    private void posicionarFrotaCompleta(Tabuleiro tab) {
        tab.posicionarNavio(FabricaNavio.criar(TipoNavio.PORTA_AVIOES, 0, 0, Direcao.HORIZONTAL));
        tab.posicionarNavio(FabricaNavio.criar(TipoNavio.ENCOURACADO, 1, 0, Direcao.HORIZONTAL));
        tab.posicionarNavio(FabricaNavio.criar(TipoNavio.CRUZADOR, 2, 0, Direcao.HORIZONTAL));
        tab.posicionarNavio(FabricaNavio.criar(TipoNavio.SUBMARINO, 3, 0, Direcao.HORIZONTAL));
        tab.posicionarNavio(FabricaNavio.criar(TipoNavio.DESTROYER, 4, 0, Direcao.HORIZONTAL));
    }
}

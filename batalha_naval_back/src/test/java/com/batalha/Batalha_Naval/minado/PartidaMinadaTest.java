package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.dominio.Direcao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PartidaMinadaTest {

    private PartidaMinada partida;

    @BeforeEach
    void setUp() {
        partida = new PartidaMinada("Alice", "MinaSala", null);
    }

    @Test
    void partidaCriadaComStatusAguardando() {
        assertEquals(StatusPartidaMinada.AGUARDANDO, partida.getStatus());
        assertEquals("Alice", partida.getJogador1());
        assertNull(partida.getJogador2());
    }

    @Test
    void jogador2Entra() {
        partida.entrar("Bob", null);
        assertEquals("Bob", partida.getJogador2());
        assertEquals(StatusPartidaMinada.POSICIONANDO, partida.getStatus());
    }

    @Test
    void naoPermiteEntrarNaPropriaSala() {
        assertThrows(IllegalStateException.class, () -> partida.entrar("Alice", null));
    }

    @Test
    void naoPermiteEntrarComSenhaErrada() {
        PartidaMinada comSenha = new PartidaMinada("Alice", "Sala", "abc");
        assertThrows(IllegalArgumentException.class, () -> comSenha.entrar("Bob", "xyz"));
    }

    @Test
    void marcarProntoSemNaviosSuficientesLancaErro() {
        partida.entrar("Bob", null);
        assertThrows(IllegalStateException.class, () -> partida.marcarPronto("Alice"));
    }

    @Test
    void marcarProntoSemMinasSuficientesLancaErro() {
        partida.entrar("Bob", null);
        posicionarTodosNavios("Alice");
        assertThrows(IllegalStateException.class, () -> partida.marcarPronto("Alice"));
    }

    @Test
    void marcarProntoComTudoPosicionadoFunciona() {
        partida.entrar("Bob", null);
        posicionarTudo("Alice");
        posicionarTudo("Bob");

        partida.marcarPronto("Alice");
        assertEquals(StatusPartidaMinada.POSICIONANDO, partida.getStatus());

        partida.marcarPronto("Bob");
        assertEquals(StatusPartidaMinada.EM_ANDAMENTO, partida.getStatus());
    }

    @Test
    void registrarChegadaIniciaContagemQuandoOsDoisChegam() {
        partida.entrar("Bob", null);
        posicionarTudo("Alice");
        posicionarTudo("Bob");
        partida.marcarPronto("Alice");
        partida.marcarPronto("Bob");

        assertFalse(partida.registrarChegada("Alice"));
        assertTrue(partida.registrarChegada("Bob"));
    }

    @Test
    void primeiroTiroEhSeguroMesmoEmMina() {
        partida.entrar("Bob", null);
        posicionarTodosNavios("Alice");
        posicionarTodasMinas("Alice");

        posicionarTodosNavios("Bob");
        partida.posicionarMina("Bob", 6, 0);
        int count = 1;
        for (int l = 8; l < 16 && count < PartidaMinada.QTD_MINAS; l++) {
            for (int c = 0; c < 16 && count < PartidaMinada.QTD_MINAS; c++) {
                partida.posicionarMina("Bob", l, c);
                count++;
            }
        }

        partida.marcarPronto("Alice");
        partida.marcarPronto("Bob");

        ResultadoTiroMinado resultado = partida.atirar("Alice", 6, 0);
        assertEquals(ResultadoTiroMinado.AGUA, resultado);
    }

    @Test
    void segundoTiroEmMinaMata() {
        partida.entrar("Bob", null);
        posicionarTodosNavios("Alice");
        posicionarTodasMinas("Alice");

        posicionarTodosNavios("Bob");
        partida.posicionarMina("Bob", 6, 0);
        partida.posicionarMina("Bob", 6, 1);
        int count = 2;
        for (int l = 8; l < 16 && count < PartidaMinada.QTD_MINAS; l++) {
            for (int c = 0; c < 16 && count < PartidaMinada.QTD_MINAS; c++) {
                partida.posicionarMina("Bob", l, c);
                count++;
            }
        }

        partida.marcarPronto("Alice");
        partida.marcarPronto("Bob");

        partida.atirar("Alice", 6, 0);
        ResultadoTiroMinado r2 = partida.atirar("Alice", 6, 1);
        assertEquals(ResultadoTiroMinado.MINA, r2);
        assertEquals(StatusPartidaMinada.FINALIZADA, partida.getStatus());
        assertEquals("Bob", partida.getVencedor());
    }

    @Test
    void acertarTodosNaviosGanha() {
        partida.entrar("Bob", null);
        posicionarTudo("Alice");

        partida.getTabuleiro2().posicionarNavio(0, 0, 5, Direcao.HORIZONTAL);
        partida.getTabuleiro2().posicionarNavio(1, 0, 4, Direcao.HORIZONTAL);
        partida.getTabuleiro2().posicionarNavio(2, 0, 3, Direcao.HORIZONTAL);
        partida.getTabuleiro2().posicionarNavio(3, 0, 3, Direcao.HORIZONTAL);
        partida.getTabuleiro2().posicionarNavio(4, 0, 2, Direcao.HORIZONTAL);
        int count = 0;
        for (int l = 8; l < 16 && count < PartidaMinada.QTD_MINAS; l++) {
            for (int c = 0; c < 16 && count < PartidaMinada.QTD_MINAS; c++) {
                partida.getTabuleiro2().posicionarMina(l, c);
                count++;
            }
        }

        partida.marcarPronto("Alice");
        partida.marcarPronto("Bob");

        for (int c = 0; c < 5; c++) partida.atirar("Alice", 0, c);
        for (int c = 0; c < 4; c++) partida.atirar("Alice", 1, c);
        for (int c = 0; c < 3; c++) partida.atirar("Alice", 2, c);
        for (int c = 0; c < 3; c++) partida.atirar("Alice", 3, c);
        for (int c = 0; c < 2; c++) partida.atirar("Alice", 4, c);

        assertEquals(StatusPartidaMinada.FINALIZADA, partida.getStatus());
        assertEquals("Alice", partida.getVencedor());
    }

    @Test
    void abandonarDaVitoriaAoOutro() {
        partida.entrar("Bob", null);
        posicionarTudo("Alice");
        posicionarTudo("Bob");
        partida.marcarPronto("Alice");
        partida.marcarPronto("Bob");

        partida.abandonar("Alice");
        assertEquals(StatusPartidaMinada.FINALIZADA, partida.getStatus());
        assertEquals("Bob", partida.getVencedor());
    }

    @Test
    void removerJogador2VoltaAguardando() {
        partida.entrar("Bob", null);
        partida.removerJogador2();
        assertNull(partida.getJogador2());
        assertEquals(StatusPartidaMinada.AGUARDANDO, partida.getStatus());
    }

    private void posicionarTudo(String jogador) {
        posicionarTodosNavios(jogador);
        posicionarTodasMinas(jogador);
    }

    private void posicionarTodosNavios(String jogador) {
        int linha = 0;
        for (int tamanho : PartidaMinada.TAMANHOS_NAVIOS) {
            partida.posicionarNavio(jogador, linha, 0, tamanho, Direcao.HORIZONTAL);
            linha++;
        }
    }

    private void posicionarTodasMinas(String jogador) {
        int count = 0;
        for (int l = 8; l < 16 && count < PartidaMinada.QTD_MINAS; l++) {
            for (int c = 0; c < 16 && count < PartidaMinada.QTD_MINAS; c++) {
                partida.posicionarMina(jogador, l, c);
                count++;
            }
        }
    }
}

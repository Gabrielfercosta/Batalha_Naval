package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.dominio.Direcao;
import com.batalha.Batalha_Naval.dto.SalaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.batalha.Batalha_Naval.dominio.StatusPartida;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MinadoServiceTest {

    private MinadoService minadoService;

    @BeforeEach
    void setUp() {
        minadoService = new MinadoService();
    }

    @Test
    void criarPartidaRetornaId() {
        String id = minadoService.criarPartida("Alice", "Sala1", null);
        assertNotNull(id);
        assertFalse(id.isBlank());
    }

    @Test
    void buscarPartidaExistente() {
        String id = minadoService.criarPartida("Alice", "Sala1", null);
        PartidaMinada p = minadoService.buscarPartida(id);
        assertEquals("Alice", p.getJogador1());
    }

    @Test
    void buscarPartidaInexistenteLancaErro() {
        assertThrows(IllegalArgumentException.class,
                () -> minadoService.buscarPartida("id-invalido"));
    }

    @Test
    void entrarNaPartida() {
        String id = minadoService.criarPartida("Alice", "Sala1", null);
        PartidaMinada p = minadoService.entrarNaPartida(id, "Bob", null);
        assertEquals("Bob", p.getJogador2());
        assertEquals(StatusPartida.POSICIONANDO, p.getStatus());
    }

    @Test
    void listarPartidasAbertas() {
        minadoService.criarPartida("Alice", "Sala1", null);
        minadoService.criarPartida("Bob", "Sala2", "senha");

        List<SalaResponse> abertas = minadoService.listarPartidasAbertas();
        assertEquals(2, abertas.size());
    }

    @Test
    void listarNaoMostraPartidasComJogador2() {
        String id = minadoService.criarPartida("Alice", "Sala1", null);
        minadoService.entrarNaPartida(id, "Bob", null);

        List<SalaResponse> abertas = minadoService.listarPartidasAbertas();
        assertEquals(0, abertas.size());
    }

    @Test
    void posicionarNavioFunciona() {
        String id = minadoService.criarPartida("Alice", "Sala1", null);
        minadoService.posicionarNavio(id, "Alice", 0, 0, 5, Direcao.HORIZONTAL);

        PartidaMinada p = minadoService.buscarPartida(id);
        assertEquals(5, p.getTabuleiro1().contarNavios());
    }

    @Test
    void posicionarMinaFunciona() {
        String id = minadoService.criarPartida("Alice", "Sala1", null);
        minadoService.posicionarMina(id, "Alice", 10, 10);

        PartidaMinada p = minadoService.buscarPartida(id);
        assertEquals(1, p.getTabuleiro1().contarMinas());
    }

    @Test
    void sairDaPartidaEmAndamentoDaVitoria() {
        String id = minadoService.criarPartida("Alice", "Sala1", null);
        minadoService.entrarNaPartida(id, "Bob", null);
        posicionarTudo(id, "Alice");
        posicionarTudo(id, "Bob");
        minadoService.marcarPronto(id, "Alice");
        minadoService.marcarPronto(id, "Bob");

        PartidaMinada p = minadoService.sairDaPartida(id, "Alice");
        assertEquals(StatusPartida.FINALIZADA, p.getStatus());
        assertEquals("Bob", p.getVencedor());
    }

    @Test
    void sairDaSalaAguardandoRemovePartida() {
        String id = minadoService.criarPartida("Alice", "Sala1", null);
        minadoService.sairDaPartida(id, "Alice");

        assertThrows(IllegalArgumentException.class,
                () -> minadoService.buscarPartida(id));
    }

    @Test
    void sairComoJogador2RemoveJogador2() {
        String id = minadoService.criarPartida("Alice", "Sala1", null);
        minadoService.entrarNaPartida(id, "Bob", null);
        minadoService.sairDaPartida(id, "Bob");

        PartidaMinada p = minadoService.buscarPartida(id);
        assertNull(p.getJogador2());
        assertEquals(StatusPartida.AGUARDANDO, p.getStatus());
    }

    private void posicionarTudo(String id, String jogador) {
        int linha = 0;
        for (int tamanho : PartidaMinada.TAMANHOS_NAVIOS) {
            minadoService.posicionarNavio(id, jogador, linha, 0, tamanho, Direcao.HORIZONTAL);
            linha++;
        }
        int count = 0;
        for (int l = 8; l < 16 && count < PartidaMinada.QTD_MINAS; l++) {
            for (int c = 0; c < 16 && count < PartidaMinada.QTD_MINAS; c++) {
                minadoService.posicionarMina(id, jogador, l, c);
                count++;
            }
        }
    }
}
